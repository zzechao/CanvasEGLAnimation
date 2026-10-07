package com.base.animation

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.base.animation.common.AnimPlayer
import com.base.animation.model.AnimPathObject

/**
 * @author:zhouzechao
 * @date: 1/8/21
 * description：普通view的canvas动画
 */
open class AnimView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, private val player: AnimPlayer = AnimPlayer()
) : View(context, attrs, defStyleAttr), IAnimView by player, CanvasHandler.CanvasFrameCallback {
    companion object {
        private const val TAG = "AnimView"
    }

    private var frameCount: Int = 0
    private var curFrameTime: Long = 0L

    init {
        background = AnimViewDrawable { drawAnimFps(it, frameCount, curFrameTime) }
    }

    /**
     * open drawAnim方法
     */
    open fun drawAnimFps(canvas: Canvas?, framePositionCount: Int, frameTime: Long) {
        drawAnim(canvas, framePositionCount, frameTime)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player.setCanvasFrameCallback(null)
        endAnimation()
        release()
    }

    override fun addAnimDisplay(animPathObject: AnimPathObject) {
        player.addAnimDisplay(animPathObject)
        player.setCanvasFrameCallback(this)
    }

    override fun getView(): View {
        return this
    }

    override fun getViewByAnimName(name: String): View? {
        return tryCatch(catchBlock = {
            Animer.log.e("AnimView", "getViewByAnimName name:$name")
        }) {
            val context = AnimationEx.mApplication ?: return@tryCatch null
            val id = context.resources.getIdentifier(
                name, "id", context.packageName
            )
            this.findFragmentOfGivenView()?.let {
                it.view?.findViewById<View>(id)
            } ?: this.getFragmentActivity()?.findViewById(id)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) touchAnimEvent(event)
        return super.dispatchTouchEvent(event)
    }

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
        frameCount = framePositionCount
        curFrameTime = frameTime
        background.invalidateSelf()
        return true
    }
}