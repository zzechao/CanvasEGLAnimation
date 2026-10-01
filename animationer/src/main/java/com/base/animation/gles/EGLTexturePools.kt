package com.base.animation.gles


import android.opengl.GLES20
import com.base.animation.Animer
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * @author zzechao
 * @date 2025/3/24 19:02
 * @desc 纹理池
 */
class EGLTexturePools {

    companion object {
        private const val TAG = "EGLTexturePools"
        private const val DISPLAYMAXCACHESIZE = 50L
    }

    private var nanoTime = 0L

    // 暂存因过期或被淘汰的纹理，在 GL 线程被调度时安全释放，杜绝显存泄漏
    private val pendingEvictedTextures = ConcurrentLinkedQueue<EGLAnimTexture>()

    private val textureCaches: Cache<Int, EGLAnimTexture> by lazy {
        CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(DISPLAYMAXCACHESIZE)
            .initialCapacity(10)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .removalListener(RemovalListener<Int, EGLAnimTexture> { notification ->
                notification.value?.let {
                    pendingEvictedTextures.offer(it)
                }
            })
            .build()
    }

    /**
     * 在 GL 线程调用，安全释放已被淘汰的纹理与表面
     */
    fun pollAndReleaseEvictedTextures() {
        while (true) {
            val item = pendingEvictedTextures.poll() ?: break
            try {
                if (item.textureId > 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(item.textureId), 0)
                    item.textureId = 0
                }
                item.surface?.release()
                item.surface = null
                item.surfaceTexture?.release()
                item.surfaceTexture = null
            } catch (e: Exception) {
                Animer.log.e(TAG, "pollAndReleaseEvictedTextures error: $e")
            }
        }
    }

    fun getTexture(bitmapHash: Int, createTexture: () -> EGLAnimTexture): EGLAnimTexture {
        pollAndReleaseEvictedTextures()
        return textureCaches.getIfPresent(bitmapHash)?.let {
            if (System.currentTimeMillis() - nanoTime > 10000) {
                nanoTime = System.currentTimeMillis()
                Animer.log.i(TAG, "$bitmapHash size:${textureCaches.size()} ${GLES20.glIsTexture(it.textureId)}")
            }
            if (GLES20.glIsTexture(it.textureId)) {
                it
            } else {
                Animer.log.i(TAG, "getTexture glIsTexture invalid $it $bitmapHash size:${textureCaches.size()}")
                if (it.textureId > 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(it.textureId), 0)
                }
                itemRelease(it)
                createTexture().apply { putTexture(bitmapHash, this) }
            }
        } ?: createTexture().also {
            putTexture(bitmapHash, it)
        }
    }

    private fun itemRelease(animTexture: EGLAnimTexture) {
        try {
            animTexture.surface?.release()
            animTexture.surface = null
            animTexture.surfaceTexture?.release()
            animTexture.surfaceTexture = null
        } catch (_: Exception) {
        }
    }

    private fun putTexture(bitmapHash: Int, textureId: EGLAnimTexture) {
        textureCaches.put(bitmapHash, textureId)
    }

    fun textureCacheMap() = textureCaches.asMap()

    fun clear() {
        textureCaches.invalidateAll()
        pollAndReleaseEvictedTextures()
    }
}