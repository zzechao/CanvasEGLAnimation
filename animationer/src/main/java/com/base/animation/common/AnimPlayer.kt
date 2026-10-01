package com.base.animation.common

import android.graphics.Canvas
import android.graphics.PointF
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.base.animation.*
import com.base.animation.helper.PathObjectDeal
import com.base.animation.helper.PathObjectDeal2
import com.base.animation.model.AnimDrawObject
import com.base.animation.model.AnimPathObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author zzechao
 * @date 2025/3/20 11:10
 */
open class AnimPlayer(
    private var isMainHandler: Boolean = true
) : IAnimView {
    companion object {
        private const val TAG = "AnimPlayer"
    }

    private val canvasHandler by lazy { CanvasHandler() }
    private var callback: CanvasHandler.CanvasFrameCallback? = null

    private var isResume = AtomicBoolean(false)
    protected var mTouchPointF: PointF? = null

    /**
     * pathObject转化
     */
    protected val pathObjectDeal by lazy {
        if (AnimationEx.mode == 1) PathObjectDeal {
            onResume()
        } else PathObjectDeal2 {
            onResume()
        }
    }

    override fun resume() {
        if (pathObjectDeal.hasTask()) {
            onResume()
        }
    }

    fun setCanvasFrameCallback(callback: CanvasHandler.CanvasFrameCallback?) {
        this.callback = callback
    }

    private fun onResume() {
        Animer.log.i(TAG, "onResume isSurfaceView:$isMainHandler $callback")
        val callback = callback ?: return
        if (isResume.compareAndSet(false, true)) {
            Animer.log.i(TAG, "onResume isSurfaceView:$isMainHandler $callback")
            if (isMainHandler) {
                ChoreographerKT.mainHandler.post { canvasHandler.setAnimationFrameCallback(callback) }
            } else {
                ChoreographerKT.animViewHandler.post { canvasHandler.setAnimationFrameCallback(callback) }
            }
        }
    }

    override fun pause() {
        isResume.getAndSet(false)
        if (isMainHandler) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                canvasHandler.removeCallback()
            } else {
                ChoreographerKT.mainHandler.post { canvasHandler.removeCallback() }
            }
        } else {
            if (Looper.myLooper() == ChoreographerKT.animViewHandler.looper) {
                canvasHandler.removeCallback()
            } else {
                ChoreographerKT.animViewHandler.post { canvasHandler.removeCallback() }
            }
        }
    }

    override fun endAnimation() {
        pause()
    }

    override fun addAnimDisplay(animPathObject: AnimPathObject) {
        pathObjectDeal.sendAnimPath(animPathObject)
    }

    override fun removeAnimId(animId: Long) {
        pathObjectDeal.removeAnimId(animId, isCancel = true)
    }


    override fun addAnimListener(iAnimListener: IAnimListener) {
        pathObjectDeal.animListeners.add(iAnimListener)
    }

    override fun removeAnimListener(iAnimListener: IAnimListener?) {
        if (iAnimListener == null) {
            pathObjectDeal.animListeners.clear()
        } else {
            pathObjectDeal.animListeners.remove(iAnimListener)
        }
    }

    override fun setOnItemClick(onItemClick: OnAnimItemClick?) {
        pathObjectDeal.onItemListener = null
        onItemClick?.let {
            pathObjectDeal.onItemListener = object : OnAnimItemClick {
                override fun itemClick(animId: Long, animDrawObject: AnimDrawObject, touchPointF: PointF, itemCenterPointF: PointF, extra: String) {
                    checkInMainThread {
                        it.itemClick(animId, animDrawObject, touchPointF, itemCenterPointF, extra)
                    }
                }
            }
        }
    }

    override fun getView(): View? = null

    override fun getViewByAnimName(name: String): View? = null

    override fun touchAnimEvent(event: MotionEvent): Boolean {
        Animer.log.i(TAG, "onTouchEvent $event")
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val (xPos: Float, yPos: Float) = event.getX(index) to event.getY(index)
                mTouchPointF = PointF(xPos, yPos)
            }
        }
        return false
    }

    override fun drawAnim(canvas: Canvas?, framePositionCount: Int, frameTime: Long) {
        canvas ?: return
        val ids = pathObjectDeal.animDrawIds.toList()
        val data = pathObjectDeal.animDrawObjects.toMap()
        if (ids.isNotEmpty()) {
            ids.forEach { data[it]?.draw(canvas, pathObjectDeal, framePositionCount, frameTime) }
            mTouchPointF?.let { DoubleLinkedReference(it) }?.let {
                val size = ids.size - 1
                for (index in size downTo 0) {
                    data[ids[index]]?.touch(pathObjectDeal, it)
                }
                mTouchPointF = null
            }
        } else if (frameTime > 0) {
            pause()
            pathObjectDeal.animDrawObjects.clear()
        }
    }

    private fun checkInMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            ChoreographerKT.mainHandler.post { block.invoke() }
        }
    }
}