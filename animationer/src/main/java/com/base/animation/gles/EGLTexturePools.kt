package com.base.animation.gles


import android.opengl.GLES20
import android.util.LruCache
import com.base.animation.Animer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author zzechao
 * @date 2025/3/24 19:02
 * @desc 纹理池
 */
class EGLTexturePools {

    companion object {
        private const val TAG = "EGLTexturePools"
        private const val DISPLAYMAXCACHESIZE = 200
    }

    private var nanoTime = 0L

    // 暂存因过期或被淘汰的纹理，在 GL 线程被调度时安全释放，杜绝显存泄漏
    private val pendingEvictedTextures = ConcurrentLinkedQueue<EGLAnimTexture>()

    private val textureCaches = object : LruCache<Int, EGLAnimTexture>(DISPLAYMAXCACHESIZE) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: EGLAnimTexture, newValue: EGLAnimTexture?) {
            pendingEvictedTextures.offer(oldValue)
        }
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

    @Synchronized
    fun getTexture(bitmapHash: Int, createTexture: () -> EGLAnimTexture): EGLAnimTexture {
        // 淘汰纹理的释放统一在 drawRenderBegin 每帧调用 pollAndReleaseEvictedTextures，这里不再逐次 poll
        val cached = textureCaches.get(bitmapHash)
        if (cached != null && cached.textureId > 0) {
            return cached
        }
        if (cached != null) {
            itemRelease(cached)
            textureCaches.remove(bitmapHash)
        }
        val newTexture = createTexture()
        if (newTexture.textureId > 0) {
            putTexture(bitmapHash, newTexture)
        }
        return newTexture
    }

    private fun itemRelease(animTexture: EGLAnimTexture) {
        try {
            if (animTexture.textureId > 0) {
                GLES20.glDeleteTextures(1, intArrayOf(animTexture.textureId), 0)
                animTexture.textureId = 0
            }
            animTexture.surface?.release()
            animTexture.surface = null
            animTexture.surfaceTexture?.release()
            animTexture.surfaceTexture = null
        } catch (e: Exception) {
        }
    }

    @Synchronized
    fun getTextureIfPresent(bitmapHash: Int): EGLAnimTexture? {
        val cached = textureCaches.get(bitmapHash)
        return if (cached != null && cached.textureId > 0) cached else null
    }

    @Synchronized
    fun putTexture(bitmapHash: Int, textureId: EGLAnimTexture) {
        if (textureId.textureId > 0) {
            textureCaches.put(bitmapHash, textureId)
        }
    }

    fun textureCacheMap() = textureCaches.snapshot()

    fun clear() {
        textureCaches.evictAll()
        pollAndReleaseEvictedTextures()
    }
}