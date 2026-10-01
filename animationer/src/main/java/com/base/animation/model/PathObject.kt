package com.base.animation.model

import android.graphics.PointF
import android.view.animation.BaseInterpolator
import android.view.animation.LinearInterpolator
import com.base.animation.Animer

class PathObject(
    var displayItemId: String,
    var point: PointF = PointF(0f, 0f),
    var alpha: Int = 255,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var rotation: Float = 0.0f,
    var interpolator: BaseInterpolator = LinearInterpolator(),
    val pointPosition: Int = 0
) {
    val tag = "PathObject"

    var itemX = 0f
    var itemY = 0f
    var itemAlpha = 0
    var itemScaleX = 0f
    var itemScaleY = 0f
    var itemRotation = 0f
    var isInitItem = false

    fun getItem(startPathObject: PathObject) {
        if (isInitItem) {
            return
        }
        isInitItem = true
        if (itemX == 0f) {
            itemX = (this.point.x - startPathObject.point.x)
        }
        if (itemY == 0f) {
            itemY = (this.point.y - startPathObject.point.y)
        }
        if (itemAlpha == 0) {
            itemAlpha = (this.alpha - startPathObject.alpha)
        }
        if (itemScaleX == 0f) {
            itemScaleX = (this.scaleX - startPathObject.scaleX)
        }
        if (itemScaleY == 0f) {
            itemScaleY = (this.scaleY - startPathObject.scaleY)
        }
        if (itemRotation == 0f) {
            itemRotation = (this.rotation - startPathObject.rotation)
        }
        Animer.log.i(tag, "startPathObject:$startPathObject pathObject:$this")
    }

    override fun toString(): String {
        return "PathObject(displayItemId=$displayItemId, point=$point, alpha=$alpha, scaleX=$scaleX, " +
                "scaleY=$scaleY, rotation=$rotation, interpolator=$interpolator, tag='$tag', " +
                "itemX=$itemX, itemY=$itemY, itemAlpha=$itemAlpha, itemScaleX=$itemScaleX, " +
                "itemScaleY=$itemScaleY, itemRotation=$itemRotation, isInitItem=$isInitItem)"
    }

    fun key(): String {
        return "x:${point.x}_y:${point.y}_${alpha}_${scaleX}_${scaleY}_${rotation}_${interpolator.javaClass.name}_${displayItemId}"
    }

    fun copy(): PathObject {
        return PathObject(
            this.displayItemId, point, alpha, scaleX,
            scaleY, rotation, interpolator, pointPosition
        )
    }
}
