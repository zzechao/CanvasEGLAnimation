package com.base.animation

import android.view.Choreographer

/**
 * @author:zhouzechao
 * @date: 2/8/21
 * description：仿AnimationHandler写的Choreographer处理机制
 */

private const val TAG = "CanvasHandler"

class CanvasHandler {

    private var mCanvasCallbacks: CanvasFrameCallback? = null
    private val mProvider: CanvasFrameCallbackProvider by lazy {
        MyFrameCallbackProvider()
    }

    private var lastTime = 0L
    private var isFrameScheduled = false

    private val mFrameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            isFrameScheduled = false
            com.base.animation.gles.utils.traceSection("Canvas_doFrame") {
                if (lastTime == 0L) {
                    lastTime = frameTimeNanos
                    doAnimationFrame(0L)
                } else {
                    val frameDuringTime = (frameTimeNanos - lastTime).coerceAtLeast(0L)
                    lastTime = frameTimeNanos
                    doAnimationFrame(frameDuringTime / 1000000)
                }
            }
            if (mCanvasCallbacks != null) {
                isFrameScheduled = true
                mProvider.postFrameCallback(this)
            } else {
                lastTime = 0L
            }
        }
    }

    fun setAnimationFrameCallback(
        callback: CanvasFrameCallback
    ) {
        Animer.log.i(TAG, "setAnimationFrameCallback $callback")
        mCanvasCallbacks = callback
        if (!isFrameScheduled) {
            lastTime = 0L
            isFrameScheduled = true
            mProvider.postFrameCallback(mFrameCallback)
        }
    }

    fun removeCallback() {
        Animer.log.i(TAG, "removeCallback")
        mCanvasCallbacks = null
        if (isFrameScheduled) {
            mProvider.removeFrameCallback(mFrameCallback)
            isFrameScheduled = false
        }
        lastTime = 0L
    }

    private fun doAnimationFrame(frameTime: Long) {
        mCanvasCallbacks?.doCanvasFrame(frameTime)
    }

    private class MyFrameCallbackProvider : CanvasFrameCallbackProvider {
        private var boundChoreographer: Choreographer? = null

        override fun postFrameCallback(callback: Choreographer.FrameCallback?) {
            val c = ChoreographerKT.getChoreographer()
            boundChoreographer = c
            callback?.let { c?.postFrameCallback(it) }
        }

        override fun removeFrameCallback(callback: Choreographer.FrameCallback?) {
            val c = boundChoreographer ?: ChoreographerKT.getChoreographer()
            callback?.let { c?.removeFrameCallback(it) }
            boundChoreographer = null
        }
    }

    interface CanvasFrameCallback {
        fun doCanvasFrame(frameTime: Long): Boolean
    }

    interface CanvasFrameCallbackProvider {
        fun postFrameCallback(callback: Choreographer.FrameCallback?)
        fun removeFrameCallback(callback: Choreographer.FrameCallback?)
    }
}