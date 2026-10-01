package com.base.animation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.base.animation.Animer.animThreadFactory
import com.base.animation.common.AnimPlayer
import com.base.animation.model.AnimPathObject
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author:zhouzechao
 * @date: 2020/11/21
 * description：SurfaceView的canvas的动画
 */
open class AnimSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, private val player: AnimPlayer = AnimPlayer(false)
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, IAnimView by player, CanvasHandler.CanvasFrameCallback {

    companion object {
        private const val TAG = "AnimSurfaceView"
    }

    private var isSurfaceRelease: Boolean = true
    private var animScope: CoroutineScope? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setZOrderOnTop(true)
        animScope = CoroutineScope(
            ThreadPoolExecutor(
            1, 1, 1000L, TimeUnit.MILLISECONDS, LinkedBlockingDeque(), animThreadFactory
        ).asCoroutineDispatcher() + SupervisorJob() + Animer.exceptionHandler)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player.setCanvasFrameCallback(null)
        endAnimation()
        animScope?.cancel()
    }

    override fun addAnimDisplay(animPathObject: AnimPathObject) {
        player.addAnimDisplay(animPathObject)
        player.setCanvasFrameCallback(this)
    }

    override fun getView(): View {
        return this
    }

    override fun getViewByAnimName(name: String): View? {
        return tryCatch {
            val context = AnimationEx.mApplication ?: return@tryCatch null
            val id = context.resources.getIdentifier(
                name, "id", context.packageName
            )
            this.findFragmentOfGivenView()?.let {
                it.view?.findViewById<View>(id)
            } ?: this.getFragmentActivity()?.findViewById(id)
        }
    }

    /**
     * open drawAnim方法
     */
    open fun drawAnimFps(canvas: Canvas?, framePositionCount: Int, frameTime: Long) {
        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // 设置画布的背景为透明
        drawAnim(canvas, framePositionCount, frameTime)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceRelease = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceRelease = true
        pause()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) touchAnimEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun doCanvasFrame(frameTime: Long): Boolean {
        if (isSurfaceRelease || !holder.surface.isValid) return true
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
        animScope?.launch {
            if (isSurfaceRelease || !holder.surface.isValid) return@launch
            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
                if (canvas != null) {
                    drawAnimFps(canvas, framePositionCount, frameTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return true
    }
}
