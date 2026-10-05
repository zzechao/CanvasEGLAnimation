package com.base.animation.model

import android.graphics.Canvas
import android.graphics.PointF
import com.base.animation.DoubleLinkedReference
import com.base.animation.gles.EGLRender
import com.base.animation.helper.IPathObjectDeal

/**
 * @author:zhouzechao
 * description：*
 */
abstract class BaseAnimDrawObject(open val extra: String) {

    var isVisible: Boolean = true

    abstract fun draw(canvas: Canvas, pathObjectDeal: IPathObjectDeal, framePositionCount: Int, frameTime: Long)

    abstract fun drawRender(render: EGLRender, pathObjectDeal: IPathObjectDeal, framePositionCount: Int, frameTime: Long)

    abstract fun touch(pathObjectDeal: IPathObjectDeal, touchPoint: DoubleLinkedReference<PointF>?)
}