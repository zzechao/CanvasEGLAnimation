package com.base.animation.model

import android.graphics.Canvas
import android.graphics.PointF
import com.base.animation.DoubleLinkedReference
import com.base.animation.gles.EGLRender
import com.base.animation.helper.IPathObjectDeal
import com.base.animation.helper.data.PathProcess
import com.base.animation.item.BaseDisplayItem

/**
 * @author:zhouzechao
 * @date: 2020/12/9
 * description：绘制节点
 */
private const val TAG = "DrawObject"

class DrawObject2(val animId: Long, override val extra: String) : BaseAnimDrawObject(extra) {

    var animDraws: MutableMap<Int, List<PathProcess>> = mutableMapOf()

    private var currencyPosition: Int = 0

    var status: Status = Status.INIT

    var curDisplayItemId = ""
    var displayItem: BaseDisplayItem? = null


    /**
     * 处理路径
     */
    private fun pathDrawable(pathObjectDeal: IPathObjectDeal, frameTime: Long, displayItemDraw: BaseDisplayItem.(AnimDrawObject) -> Unit) {
        var updateCurrencyPosition = true
        if (currencyPosition == 0 && status == Status.INIT) {
            status = Status.START
            pathObjectDeal.animListeners.forEach { it.onStartAnim(animId, extra) }
        } else if (status == Status.START) {
            status = Status.DRAWING
            pathObjectDeal.animListeners.forEach { it.onRunningAnim(animId, extra) }
        }
        animDraws[currencyPosition]?.forEach { drawObject ->
            if (drawObject.start.displayItemId != curDisplayItemId || displayItem == null) {
                curDisplayItemId = drawObject.start.displayItemId
                displayItem = pathObjectDeal.getDisplayItem(drawObject.start.displayItemId)
            }
            displayItem?.apply {
                drawObject.curTotalTime += frameTime
                if (drawObject.curTotalTime > drawObject.durTime) {
                    drawObject.curTotalTime = drawObject.durTime * 1f
                }
                if (isCalculate) { // 是否计算任务回调上层触发
                    calculate(drawObject, drawObject.current, drawObject.interpolator)
                } else {
                    val p = if (drawObject.durTime <= 0f) 1f else (drawObject.curTotalTime / drawObject.durTime).coerceIn(0f, 1f)
                    val interP = drawObject.interpolator.getInterpolation(p)
                    val inPoint = PointF(
                        drawObject.start.point.x + drawObject.item.totalX * interP,
                        drawObject.start.point.y + drawObject.item.totalY * interP
                    )
                    val alpha = drawObject.start.alpha + (drawObject.item.totalAlpha * interP).toInt()
                    val scaleX = drawObject.start.scaleX + drawObject.item.totalScaleX * interP
                    val scaleY = drawObject.start.scaleY + drawObject.item.totalScaleY * interP
                    val rotation = drawObject.start.rotation + drawObject.item.totalRotation * interP
                    drawObject.current.reset(inPoint, alpha, scaleX, scaleY, rotation)
                }
                displayItemDraw.invoke(this, drawObject.current)
            }
            if (drawObject.curTotalTime < drawObject.durTime) {
                updateCurrencyPosition = false
            }
        }?.apply {
            if (updateCurrencyPosition) {
                currencyPosition += 1
            }
        } ?: kotlin.run {
            status = Status.STOP
            pathObjectDeal.animListeners.forEach {
                it.onEndAnim(animId, extra)
            }
            pathObjectDeal.removeAnimId(animId, isCancel = false)
        }
    }


    override fun draw(canvas: Canvas, pathObjectDeal: IPathObjectDeal, framePositionCount: Int, frameTime: Long) {
        pathDrawable(pathObjectDeal, frameTime) { drawObject ->
            draw(canvas, drawObject.point.x, drawObject.point.y, drawObject.alpha, drawObject.scaleX, drawObject.scaleY, drawObject.rotation)
        }
    }

    override fun drawRender(render: EGLRender, pathObjectDeal: IPathObjectDeal, framePositionCount: Int, frameTime: Long) {
        pathDrawable(pathObjectDeal, frameTime) { drawObject ->
            drawRender(
                animId,
                render,drawObject.point.x, drawObject.point.y, drawObject.alpha,
                drawObject.scaleX, drawObject.scaleY, drawObject.rotation
            )
        }
    }

    override fun touch(pathObjectDeal: IPathObjectDeal, touchPoint: DoubleLinkedReference<PointF>?) {
        if (status == Status.START || status == Status.DRAWING) {
            animDraws[currencyPosition]?.forEach { drawObject ->
                if (drawObject.clickable && touchPoint != null && pathObjectDeal.onItemListener != null) {
                    if (drawObject.start.displayItemId != curDisplayItemId || displayItem == null) {
                        curDisplayItemId = drawObject.start.displayItemId
                        displayItem = pathObjectDeal.getDisplayItem(drawObject.start.displayItemId)
                    }
                    displayItem?.touch(animId, pathObjectDeal.onItemListener!!, drawObject.current, touchPoint, drawObject.extra)
                }
            }
        }
    }
}