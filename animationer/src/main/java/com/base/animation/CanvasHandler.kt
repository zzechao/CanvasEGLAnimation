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
            if (lastTime == 0L) {
                lastTime = System.nanoTime()
                doAnimationFrame(0L)
            } else {
                val curFrameTime = System.nanoTime()
                val frameDuringTime = curFrameTime - lastTime
                lastTime = curFrameTime
                doAnimationFrame(frameDuringTime / 1000000)
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
        Animer.log.i(TAG,"doAnimationFrame frameTime:$frameTime")
        mCanvasCallbacks?.doCanvasFrame(frameTime)
    }

    private class MyFrameCallbackProvider : CanvasFrameCallbackProvider {
        override fun postFrameCallback(callback: Choreographer.FrameCallback?) {
            callback?.let { ChoreographerKT.getChoreographer()?.postFrameCallback(it) }
        }

        override fun removeFrameCallback(callback: Choreographer.FrameCallback?) {
            callback?.let { ChoreographerKT.getChoreographer()?.removeFrameCallback(it) }
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