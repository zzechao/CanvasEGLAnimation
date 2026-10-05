package com.base.animation

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.base.animation.AnimView.Companion
import com.base.animation.model.AnimPathObject

/**
 * @author:zhouzechao
 * @date: 1/22/21
 * description：canvas view的接口类，提供各种信息
 */
interface IAnimView {

    fun resume()

    fun pause()

    fun endAnimation()

    fun addAnimDisplay(animPathObject: AnimPathObject)

    fun removeAnimId(animId: Long)

    fun setAnimVisible(animId: Long, isVisible: Boolean)

    fun addAnimListener(iAnimListener: IAnimListener)

    fun removeAnimListener(iAnimListener: IAnimListener?)

    fun setOnItemClick(onItemClick: OnAnimItemClick?)

    fun getView(): View?

    fun getViewByAnimName(name: String): View?

    fun drawAnim(canvas: Canvas?, framePositionCount: Int, frameTime: Long)

    fun touchAnimEvent(event: MotionEvent): Boolean
}