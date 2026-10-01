package com.base.animation.item

import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import com.base.animation.DoubleLinkedReference
import com.base.animation.OnAnimItemClick
import com.base.animation.gles.EGLAnimTexture
import com.base.animation.gles.EGLRender
import com.base.animation.model.AnimDrawObject


/**
 * @author:zhouzechao
 * @date: 2020/12/23
 * description：单个bitmap的绘制item
 */


open class BitmapDisplayItem : BaseDisplayItem() {

    private val paint by lazy {
        Paint().apply {
            isAntiAlias = false
        }
    }


    private val mDisplayRect = RectF()
    private var displaySizeSet = false

    open var mBitmap: Bitmap? = null

    open fun setBitmap(bitmap: Bitmap) {
        mBitmap = bitmap
        if (!displaySizeSet) {
            displayWidth = bitmap.width
            displayHeight = bitmap.height
        }
    }

    override fun drawDisplayItem(canvas: Canvas, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float) {
        mBitmap.takeUnless { mBitmap?.isRecycled == true }?.let {
            paint.alpha = alpha.coerceIn(0, 255)
            val safeScaleX = if (scaleX == 0f) 1f else scaleX
            val safeScaleY = if (scaleY == 0f) 1f else scaleY
            val drawX = x - (displayWidth / safeScaleX / 2f)
            val drawY = y - (displayHeight / safeScaleY / 2f)
            mDisplayRect.set(drawX, drawY, drawX + displayWidth, drawY + displayHeight)
            canvas.drawBitmap(it, null, mDisplayRect, paint)
        }
    }

    override fun drawDisplayItem(animId: Long, render: EGLRender, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float, rotation: Float) {
        val bitmap = mBitmap ?: return
        if (bitmap.isRecycled) return
        val cacheKey = bitmap.hashCode()
        render.drawItem(animId, cacheKey, displayWidth, displayHeight, x, y, alpha, scaleX, scaleY, rotation, ::getTextureIfPresent)
    }

    private fun getTextureIfPresent(): EGLAnimTexture {
        val texture2DId = IntArray(1)
        val bitmap = mBitmap
        return if (bitmap != null && !bitmap.isRecycled) {
            //生成纹理
            GLES20.glGenTextures(1, texture2DId, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2DId[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            EGLAnimTexture(texture2DId[0], EGLAnimTexture.TextureType.BITMAP)
        } else EGLAnimTexture()
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

    override fun setDisplaySize(displayWidth: Int, displayHeight: Int) {
        if (displayWidth < 0 && displayHeight < 0) {
            return
        }
        this.displayWidth = displayWidth
        this.displayHeight = displayHeight
        displaySizeSet = true
    }

    override fun recycle() {
        super.recycle()
        displaySizeSet = false
        mDisplayRect.setEmpty()
        mBitmap = null
    }
}