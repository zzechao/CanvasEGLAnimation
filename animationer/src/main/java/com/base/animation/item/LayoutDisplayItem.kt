package com.base.animation.item

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.base.animation.Animer
import com.base.animation.DoubleLinkedReference
import com.base.animation.OnAnimItemClick
import com.base.animation.gles.EGLAnimTexture
import com.base.animation.gles.EGLRender
import com.base.animation.model.AnimDrawObject
import kotlin.math.*


/**
 * @author:zhouzechao
 * @date: 2020/12/23
 * description：单个view的绘制item
 */
class LayoutDisplayItem(val context: Context, private val layout: Int) : BaseDisplayItem() {

    private val view: View by lazy {
        LayoutInflater.from(context.applicationContext).inflate(layout, null)
    }

    init {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(displayHeight, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        displayWidth = view.measuredWidth
        displayHeight = view.measuredHeight
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        Animer.log.d(tag, "LayoutDisplayItem: $layout ${view.measuredWidth} ${view.measuredHeight}")
    }

    override fun drawDisplayItem(
        canvas: Canvas, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float
    ) {
        val safeScaleY = if (scaleY == 0f) 1f else scaleY
        val drawY = y - (displayHeight / safeScaleY / 2f)
        canvas.translate(x, drawY)
        view.alpha = (alpha / 255f).coerceIn(0f, 1f)
        view.draw(canvas)
    }

    override fun drawDisplayItem(animId: Long, render: EGLRender, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float, rotation: Float) {
        val maxSize = max(displayWidth, displayHeight).coerceAtLeast(1)
        val cacheKey = if (displayItemId.isNotEmpty()) displayItemId.hashCode() else this.hashCode()
        render.drawItem(animId, cacheKey, maxSize, maxSize, x, y, alpha, scaleX, scaleY, rotation, ::getTextureIfPresent, cullable = true)
    }

    private fun getTextureIfPresent(): EGLAnimTexture {
        val maxSize = max(displayWidth, displayHeight).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.withTranslation((maxSize - displayWidth) / 2f, (maxSize - displayHeight) / 2f) {
            view.draw(canvas)
        }
        val texture2DId = IntArray(1)
        GLES20.glGenTextures(1, texture2DId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2DId[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glFlush()
        bitmap.recycle()
        return EGLAnimTexture(texture2DId[0], EGLAnimTexture.TextureType.BITMAP)
    }

    override fun getRotatePX(rotation: Float, scaleX: Float): Float {
        return displayWidth / 2f
    }

    override fun getRotatePY(rotation: Float, scaleY: Float): Float {
        return displayHeight / 2f
    }

    override fun getScalePX(scaleX: Float): Float {
        return displayWidth / 2f
    }

    override fun getScalePY(scaleY: Float): Float {
        return displayHeight / 2f
    }

    override fun touch(animId: Long, onAnimItemClick: OnAnimItemClick, animDrawObject: AnimDrawObject, touchPoint: DoubleLinkedReference<PointF>, extra: String) {
        val scaledWidth = displayWidth * animDrawObject.scaleX
        val scaledHeight = displayHeight * animDrawObject.scaleY
        val left = animDrawObject.point.x - scaledWidth / 2
        val right = animDrawObject.point.x + scaledWidth / 2
        val top = animDrawObject.point.y - scaledHeight / 2
        val bottom = animDrawObject.point.y + scaledHeight / 2
        touchPoint.data?.let {
            if (it.x in left..right && it.y in top..bottom) {
                onAnimItemClick.itemClick(animId, animDrawObject, it, animDrawObject.point, extra)
                touchPoint.reset()
            }
        }
    }
}