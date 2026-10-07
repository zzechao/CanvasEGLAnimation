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
import com.base.animation.common.AnimPlayer
import com.base.animation.model.AnimPathObject
import java.util.concurrent.atomic.AtomicBoolean

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

    enum class ZOrderMode {
        NORMAL,
        MEDIA_OVERLAY,
        ON_TOP
    }

    var zOrderMode: ZOrderMode = ZOrderMode.NORMAL
        set(value) {
            field = value
            applyZOrder()
        }

    @Volatile
    private var isSurfaceRelease: Boolean = true
    private val isDrawing = AtomicBoolean(false)

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    private fun applyZOrder() {
        when (zOrderMode) {
            ZOrderMode.MEDIA_OVERLAY -> {
                super.setZOrderOnTop(false)
                super.setZOrderMediaOverlay(true)
            }
            ZOrderMode.ON_TOP -> {
                super.setZOrderMediaOverlay(false)
                super.setZOrderOnTop(true)
            }
            ZOrderMode.NORMAL -> {
                super.setZOrderMediaOverlay(false)
                super.setZOrderOnTop(false)
            }
        }
    }

    override fun setZOrderOnTop(onTop: Boolean) {
        zOrderMode = if (onTop) ZOrderMode.ON_TOP else ZOrderMode.NORMAL
        super.setZOrderOnTop(onTop)
    }

    override fun setZOrderMediaOverlay(isMediaOverlay: Boolean) {
        zOrderMode = if (isMediaOverlay) ZOrderMode.MEDIA_OVERLAY else ZOrderMode.NORMAL
        super.setZOrderMediaOverlay(isMediaOverlay)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyZOrder()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player.setCanvasFrameCallback(null)
        endAnimation()
        isDrawing.set(false)
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
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceRelease = true
        isDrawing.set(false)
        pause()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) touchAnimEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun doCanvasFrame(frameTime: Long): Boolean {
        if (isSurfaceRelease || !holder.surface.isValid) return true
        if (!isDrawing.compareAndSet(false, true)) {
            return true
        }
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
        var canvas: Canvas? = null
        try {
            if (isSurfaceRelease || !holder.surface.isValid) {
                return true
            }
            canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    holder.lockHardwareCanvas()
                } catch (e: Throwable) {
                    Animer.log.w(TAG, "lockHardwareCanvas skipped: ${e.message}")
                    if (!isSurfaceRelease && holder.surface.isValid) {
                        try {
                            holder.lockCanvas()
                        } catch (t: Throwable) {
                            null
                        }
                    } else null
                }
            } else {
                try {
                    holder.lockCanvas()
                } catch (t: Throwable) {
                    null
                }
            }
            if (canvas != null) {
                drawAnimFps(canvas, framePositionCount, frameTime)
            }
        } catch (e: Exception) {
            Animer.log.e(TAG, "drawAnimFps error: $e")
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Animer.log.w(TAG, "unlockCanvasAndPost error: $e")
                }
            }
            isDrawing.set(false)
        }
        return true
    }
}
