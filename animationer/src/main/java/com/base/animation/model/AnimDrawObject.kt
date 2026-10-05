package com.base.animation.model

import android.graphics.Canvas
import android.graphics.PointF
import com.base.animation.DoubleLinkedReference
import com.base.animation.gles.EGLRender
import com.base.animation.helper.IPathObjectDeal
import com.base.animation.helper.PathObjectDeal
import com.base.animation.item.BaseDisplayItem

/**
 * @author:zhouzechao
 * @date: 2020/12/9
 * description：绘制节点
 */
private const val TAG = "DrawObject"

class DrawObject(val animId: Long, override val extra: String) : BaseAnimDrawObject(extra) {

    var animDraws: MutableMap<Int, MutableList<AnimDrawObject>> = mutableMapOf()

    private var currencyPosition: Int = 0

    var status: Status = Status.INIT

    var curDisplayItemId = ""
    var displayItem: BaseDisplayItem? = null

    /**
     * 处理路径
     */
    private fun pathDrawable(pathObjectDeal: IPathObjectDeal, framePositionCount: Int, displayItemDraw: BaseDisplayItem.(AnimDrawObject) -> Unit) {
        if (currencyPosition == 0 && status == Status.INIT) {
            status = Status.START
            pathObjectDeal.animListeners.forEach {
                it.onStartAnim(animId, extra)
            }
        } else if (status == Status.START) {
            status = Status.DRAWING
            pathObjectDeal.animListeners.forEach {
                it.onRunningAnim(animId, extra)
            }
        } else if (status != Status.STOP && currencyPosition >= animDraws.size - 1) {
            currencyPosition = animDraws.size - 1
            status = Status.STOP
            pathObjectDeal.animListeners.forEach {
                it.onEndAnim(animId, extra)
            }
            pathObjectDeal.removeAnimId(animId, isCancel = false)
        }

        animDraws[currencyPosition]?.forEach { drawObject ->
            if (drawObject.displayItemId != curDisplayItemId || displayItem == null) {
                curDisplayItemId = drawObject.displayItemId
                displayItem = pathObjectDeal.getDisplayItem(drawObject.displayItemId)
            }
            displayItem?.let { displayItemDraw(it, drawObject) }
        }

        currencyPosition += framePositionCount
    }

    override fun draw(canvas: Canvas, pathObjectDeal: IPathObjectDeal, framePositionCount: Int, frameTime: Long) {
        pathDrawable(pathObjectDeal, framePositionCount) { drawObject ->
            if (isVisible) {
                draw(canvas, drawObject.point.x, drawObject.point.y, drawObject.alpha, drawObject.scaleX, drawObject.scaleY, drawObject.rotation)
            }
        }
    }

    override fun drawRender(render: EGLRender, pathObjectDeal: IPathObjectDeal, framePositionCount: Int, frameTime: Long) {
        pathDrawable(pathObjectDeal, framePositionCount) { drawObject ->
            if (isVisible) {
                drawRender(animId, render, drawObject.point.x, drawObject.point.y, drawObject.alpha, drawObject.scaleX, drawObject.scaleY, drawObject.rotation)
            }
        }
    }

    override fun touch(pathObjectDeal: IPathObjectDeal, touchPoint: DoubleLinkedReference<PointF>?) {
        if (!isVisible) return
        if (status == Status.START || status == Status.DRAWING) {
            val touchPosition = (currencyPosition - 1).coerceAtLeast(0)
            animDraws[touchPosition]?.forEach { drawObject ->
                if (drawObject.displayItemId != curDisplayItemId || displayItem == null) {
                    curDisplayItemId = drawObject.displayItemId
                    displayItem = pathObjectDeal.getDisplayItem(drawObject.displayItemId)
                }
                displayItem?.apply {
                    if (drawObject.clickable && touchPoint != null && pathObjectDeal.onItemListener != null) {
                        touch(animId, pathObjectDeal.onItemListener!!, drawObject, touchPoint, drawObject.expand)
                    }
                }
            }
        }
    }
}

data class AnimDrawObject(
    var displayItemId: String, var point: PointF = PointF(0f, 0f), var alpha: Int = 100, var scaleX: Float = 1.0f, var scaleY: Float = 1.0f, var rotation: Float = 0.0f, var clickable: Boolean = false, var expand: String = ""
) {
    fun reset(point: PointF, alpha: Int, scaleX: Float, scaleY: Float, rotation: Float) {
        this.point = point
        this.alpha = alpha
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.rotation = rotation
    }
}

/**
 * 转化
 */
fun PathObject.toAnimDrawObject(clickable: Boolean, expand: String): AnimDrawObject {
    return AnimDrawObject(
        displayItemId, point, alpha, scaleX, scaleY, rotation, clickable = clickable, expand = expand
    )
}

fun PathObject.toAnimDrawObject2(): AnimDrawObject {
    return AnimDrawObject(
        displayItemId, point, alpha, scaleX, scaleY, rotation
    )
}

enum class Status(val value: Int) {
    INIT(0), START(1), DRAWING(2), STOP(3)
}