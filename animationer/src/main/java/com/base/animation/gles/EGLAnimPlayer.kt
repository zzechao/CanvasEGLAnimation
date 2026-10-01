package com.base.animation.gles

import android.graphics.SurfaceTexture
import com.base.animation.Animer
import com.base.animation.CanvasHandler
import com.base.animation.DoubleLinkedReference
import com.base.animation.common.AnimPlayer
import com.base.animation.fpsTime
import com.base.animation.model.AnimPathObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.Executors

/**
 * @author zzechao
 * @date 2025/3/19 15:46
 * @description EGL动画播放
 * @param render 渲染器
 * @param glScope 协程作用域
 * @param glActor 协程通道
 */
class EGLAnimPlayer(private val render: EGLRender = EGLRender()) : AnimPlayer(false), IRenderer by render, CanvasHandler.CanvasFrameCallback {

    companion object {
        private const val TAG = "EGLAnimPlayer"
    }

    private var glScope: CoroutineScope? = null
    private var glActor: SendChannel<EGLAction>? = null

    override fun resume() {
        safeOffer(EGLAction(EGLAction.MSG_RESUME) { super.resume() })
    }

    override fun pause() {
        safeOffer(EGLAction(EGLAction.MSG_PAUSE) { super.pause() })
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        safeOffer(EGLAction(EGLAction.MSG_INIT) {
            render.onSurfaceTextureAvailable(surface, width, height)
        }).onFailure { release() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        safeOffer(EGLAction(EGLAction.MSG_SIZE_CHANGED) {
            render.onSurfaceTextureSizeChanged(surface, width, height)
        })
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        setCanvasFrameCallback(null)
        val result = safeOffer(EGLAction(EGLAction.MSG_DESTROY) {
            release()
            try {
                surface.release()
            } catch (e: Throwable) {
                Animer.log.e(TAG, "surface release error: ${e.message}")
            }
        })
        if (result.isFailure || glActor?.isClosedForSend == true) {
            release()
            try {
                surface.release()
            } catch (e: Throwable) {
                Animer.log.e(TAG, "surface release error: ${e.message}")
            }
        }
        return false
    }

    private var nanoTime = 0L
    override fun doCanvasFrame(frameTime: Long): Boolean {
        val framePositionCount = if (frameTime == 0L) {
            1
        } else {
            val framePositionCount = frameTime / fpsTime
            if (framePositionCount <= 1) {
                1
            } else {
                framePositionCount.toInt()
            }
        }
        safeOffer(EGLAction(EGLAction.MSG_PLAY) {
            val ids = pathObjectDeal.animDrawIds.toList()
            val data = pathObjectDeal.animDrawObjects.toMap()
            if (System.currentTimeMillis() - nanoTime > 10000) {
                nanoTime = System.currentTimeMillis()
                Animer.log.d(TAG, "doCanvasFrameMSG_PLAY ids:$ids")
            }
            if (ids.isNotEmpty()) {
                render.drawRenderBegin()
                kotlin.runCatching {
                    ids.forEach { data[it]?.drawRender(render, pathObjectDeal, framePositionCount, frameTime) }
                }
                render.drawRenderEnd()
                mTouchPointF?.let { DoubleLinkedReference(it) }?.let {
                    val size = ids.size - 1
                    for (index in size downTo 0) {
                        data[ids[index]]?.touch(pathObjectDeal, it)
                    }
                    mTouchPointF = null
                }
            } else {
                pause()
                setCanvasFrameCallback(null)
                render.drawRenderBegin()
                render.drawRenderEnd()
                pathObjectDeal.animDrawObjects.clear()
            }
        })
        return true
    }

    override fun addAnimDisplay(animPathObject: AnimPathObject) {
        super.addAnimDisplay(animPathObject)
        setCanvasFrameCallback(this)
    }

    fun onDetachedFromWindow() {
        safeOffer(EGLAction(EGLAction.MSG_RELEASE) {
            release()
            glActor?.close()
            glScope?.cancel()
        })
    }

    private fun release() {
        setCanvasFrameCallback(null)
        endAnimation()
        render.release()
    }

    fun onAttachedToWindow() {
        initActor()
    }

    private fun initActor() {
        glActor?.close()
        glScope?.cancel()
        glScope = CoroutineScope(
            Executors.newSingleThreadExecutor().asCoroutineDispatcher() + Animer.exceptionHandler
        )
        glActor = glScope?.actor(capacity = 50) {
            for (msg in channel) {
                Animer.log.d(TAG, "actor EGLAction:${msg.description()} run")
                msg.action()
            }
        }
        glActor?.invokeOnClose {
            release()
        }
    }

    private fun safeOffer(msg: EGLAction): Result<Unit> {
        return kotlin.runCatching { glActor?.offer(msg) }
    }
}