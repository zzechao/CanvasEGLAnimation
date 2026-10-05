package com.base.animation.item

import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import android.text.*
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.text.TextDirectionHeuristicsCompat
import com.base.animation.Animer
import com.base.animation.DoubleLinkedReference
import com.base.animation.OnAnimItemClick
import com.base.animation.gles.EGLAnimTexture
import com.base.animation.gles.EGLRender
import com.base.animation.model.AnimDrawObject
import kotlin.math.max

/**
 * @author:zhouzechao
 * @date: 2020/12/23
 * description：单个view的绘制item
 */
class StringDisplayItem(
    fontSize: Int, message: String, txtColor: Int, private val maxWidth: Int, private val singleLine: Boolean
) : BaseDisplayItem() {

    private val paint by lazy {
        TextPaint().apply {
            color = txtColor
            style = Paint.Style.FILL
            textSize = fontSize.toFloat()
        }
    }

    private var txtStaticLayout: StaticLayout? = null

    init {
        displayHeight = (fontSize + 10)
        displayWidth = fontSize * message.length
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            txtStaticLayout = StaticLayout.Builder.obtain(
                message, 0, message.length, paint, if (maxWidth > 0) maxWidth else displayWidth
            ).apply {
                if (singleLine) {
                    setMaxLines(1)
                    setEllipsize(TextUtils.TruncateAt.END)
                    setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                }
            }.build()
        } else if (singleLine) {
            txtStaticLayout = StaticLayout(
                message, 0, message.length, paint, if (maxWidth > 0) maxWidth else displayWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.END, if (maxWidth > 0) maxWidth else displayWidth
            )
        } else {
            txtStaticLayout = StaticLayout(
                message, 0, message.length, paint, if (maxWidth > 0) maxWidth else displayWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
            )
            displayHeight = (txtStaticLayout?.lineCount ?: 1) * (fontSize + 10)
        }
    }

    override fun drawDisplayItem(canvas: Canvas, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float) {
        txtStaticLayout?.paint?.alpha = alpha
        canvas.translate(x, y)
        txtStaticLayout?.draw(canvas)
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
            txtStaticLayout?.draw(this)
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
        return if (maxWidth > 0) maxWidth / 2f else displayWidth / 2f
    }

    override fun getScalePY(scaleY: Float): Float {
        return if (maxWidth > 0 && !singleLine) (txtStaticLayout?.lineCount ?: 1) * displayHeight / 2f else displayHeight / 2f
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