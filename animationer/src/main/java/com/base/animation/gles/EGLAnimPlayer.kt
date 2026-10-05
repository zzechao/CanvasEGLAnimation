package com.base.animation.gles

import android.graphics.SurfaceTexture
import android.os.Handler
import com.base.animation.Animer
import com.base.animation.CanvasHandler
import com.base.animation.ChoreographerKT
import com.base.animation.DoubleLinkedReference
import com.base.animation.common.AnimPlayer
import com.base.animation.fpsTime
import com.base.animation.model.AnimPathObject

/**
 * @author zzechao
 * @date 2025/3/19 15:46
 * @description EGL动画播放器（方案A：Choreographer 硬件直驱模式）
 * 核心特性：
 * 1. Choreographer 硬件 VSYNC 直接入驻 EGL-GroupThread 渲染线程；
 * 2. 0 次跨线程中转，VSYNC 触发后立即就地渲染并 swapBuffers；
 * 3. 彻底淘汰中间 HandlerThread 和协程 Channel 消息队列，0 消息延迟，0 额外对象分配。
 * @param render 渲染器
 */
class EGLAnimPlayer(private val render: EGLRender = EGLRender()) : AnimPlayer(false), IRenderer by render, CanvasHandler.CanvasFrameCallback {

    companion object {
        private const val TAG = "EGLAnimPlayer"
    }

    private var renderGroup: EGLRenderGroup? = null

    /**
     * 将动画节拍 Looper 线程定向到本 EGL 组专属的 GL 工作线程
     */
    override fun getAnimHandler(): Handler {
        return renderGroup?.handler ?: ChoreographerKT.animViewHandler
    }

    override fun resume() {
        renderGroup?.handler?.post { super.resume() } ?: super.resume()
    }

    override fun pause() {
        super.pause()
    }

    override fun endAnimation() {
        super.endAnimation()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (renderGroup == null || renderGroup!!.isReleased.get()) {
            initGroup()
        }
        renderGroup?.handler?.post {
            render.onSurfaceTextureAvailable(surface, width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderGroup?.handler?.post {
            render.onSurfaceTextureSizeChanged(surface, width, height)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        setCanvasFrameCallback(null)
        val group = renderGroup
        if (group != null && !group.isReleased.get()) {
            group.handler.post {
                release()
                cleanupGroup()
                try {
                    surface.release()
                } catch (e: Throwable) {
                    Animer.log.e(TAG, "surface release error: ${e.message}")
                }
            }
        } else {
            release()
            cleanupGroup()
            try {
                surface.release()
            } catch (e: Throwable) {
                Animer.log.e(TAG, "surface release error: ${e.message}")
            }
        }
        return false
    }

    /**
     * 关键渲染回调：由当前 GL 线程的 Choreographer 硬件 VSYNC 直接就地调用！
     * 0 跨线程延迟，0 任务打包分配，立即执行 GPU 渲染
     */
    override fun doCanvasFrame(frameTime: Long): Boolean {
        return com.base.animation.gles.utils.traceSection("EGL_doCanvasFrame") {
            val framePositionCount = if (frameTime <= 0L) {
                1
            } else {
                val count = (frameTime / fpsTime).toInt()
                if (count <= 1) 1 else count
            }

            try {
                val ids = pathObjectDeal.animDrawIds
                val data = pathObjectDeal.animDrawObjects
                if (ids.isNotEmpty()) {
                    com.base.animation.gles.utils.traceSection("EGL_Frame_Render") {
                        com.base.animation.gles.utils.traceSection("EGL_drawRenderBegin") {
                            render.drawRenderBegin()
                        }
                        try {
                            com.base.animation.gles.utils.traceSection("EGL_drawItems_all") {
                                val size = ids.size
                                for (i in 0 until size) {
                                    val id = ids.getOrNull(i) ?: continue
                                    data[id]?.drawRender(render, pathObjectDeal, framePositionCount, frameTime)
                                }
                            }
                        } catch (t: Throwable) {
                            // ignore render exception
                        }
                        com.base.animation.gles.utils.traceSection("EGL_drawRenderEnd_and_swap") {
                            render.drawRenderEnd()
                        }
                        mTouchPointF?.let { DoubleLinkedReference(it) }?.let {
                            com.base.animation.gles.utils.traceSection("EGL_touch_check") {
                                val size = ids.size
                                for (i in size - 1 downTo 0) {
                                    val id = ids.getOrNull(i) ?: continue
                                    data[id]?.touch(pathObjectDeal, it)
                                }
                                mTouchPointF = null
                            }
                        }
                    }
                } else {
                    pause()
                    render.drawRenderBegin()
                    render.drawRenderEnd()
                    pathObjectDeal.animDrawObjects.clear()
                }
            } catch (e: Throwable) {
                Animer.log.e(TAG, "render error: $e")
            }
            true
        }
    }

    override fun addAnimDisplay(animPathObject: AnimPathObject) {
        if (renderGroup == null || renderGroup!!.isReleased.get()) {
            initGroup()
        }
        setCanvasFrameCallback(this)
        super.addAnimDisplay(animPathObject)
    }

    fun onAttachedToWindow() {
        setCanvasFrameCallback(this)
        initGroup()
    }

    fun onDetachedFromWindow() {
        setCanvasFrameCallback(null)
        val group = renderGroup
        if (group != null && !group.isReleased.get()) {
            group.handler.post {
                release()
                cleanupGroup()
            }
        } else {
            release()
            cleanupGroup()
        }
    }

    private fun release() {
        setCanvasFrameCallback(null)
        endAnimation()
        render.release()
    }

    @Synchronized
    private fun initGroup() {
        if (renderGroup != null && !renderGroup!!.isReleased.get()) {
            return
        }
        cleanupGroup()
        val group = EGLGroupManager.obtainGroup()
        renderGroup = group
        render.attachGroup(group)
    }

    @Synchronized
    private fun cleanupGroup() {
        val group = renderGroup ?: return
        renderGroup = null
        EGLGroupManager.releaseGroup(group)
    }
}