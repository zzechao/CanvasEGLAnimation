package com.base.animation

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Choreographer


/**
 * @author:zhouz
 * @date: 2024/7/1 17:55
 * description：根据是否SurfaceView 区分 post frame的 looper
 */
object ChoreographerKT {
    internal val mainHandler = Handler(Looper.getMainLooper())
    private var mainChoreographer: Choreographer? = null

    internal val animViewHandler: Handler by lazy {
        val handlerThread = HandlerThread("AnimPlayer_Handler")
        handlerThread.start()
        Handler(handlerThread.looper)
    }

    /**
     * 根据不同looper 构造
     */
    fun getChoreographer(): Choreographer? {
        val looper = Looper.myLooper() ?: return null
        return if (looper == Looper.getMainLooper()) {
            if (mainChoreographer == null) {
                mainChoreographer = Choreographer.getInstance()
            }
            mainChoreographer
        } else if (looper == animViewHandler.looper) {
            Choreographer.getInstance()
        } else {
            null
        }
    }
}