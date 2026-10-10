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
    
    fun release() {}

    /**
     * 动态移除指定 animId 的路径与渲染缓存
     * @param animId 动画唯一 ID
     * @param removeDisplayItem 是否同步移除 DisplayItem（默认 false，避免误伤其他正在播放的共享资源）
     * @param clearLayoutPointCache 是否同步清除 View 布局坐标缓存（默认 false，麦位移动时可传入 true）
     */
    fun removeAnimCache(animId: Long, removeDisplayItem: Boolean = false, clearLayoutPointCache: Boolean = false)

    /**
     * 批量动态移除 animId 列表的路径与渲染缓存
     */
    fun removeAnimCache(animIds: Collection<Long>, removeDisplayItem: Boolean = false, clearLayoutPointCache: Boolean = false)
}