package com.base.animation.helper

import com.base.animation.AnimCache
import com.base.animation.IAnimListener
import com.base.animation.OnAnimItemClick
import com.base.animation.item.BaseDisplayItem
import com.base.animation.model.AnimPathObject
import com.base.animation.model.BaseAnimDrawObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author:zhouzechao
 * description：*
 */
interface IPathObjectDeal {

    val animDrawIds: MutableList<Long>
    val animDrawObjects: ConcurrentHashMap<Long, BaseAnimDrawObject>

    val animListeners: MutableSet<IAnimListener>

    var onItemListener: OnAnimItemClick?

    fun getDisplayItem(displayItemId: String): BaseDisplayItem? {
        return AnimCache.displayItemCache.getDisplayItem(displayItemId)
    }

    fun sendAnimPath(animPathObject: AnimPathObject)

    fun hasTask(): Boolean

    fun removeAnimId(animId: Long, isCancel: Boolean = false)

    fun setAnimVisible(animId: Long, isVisible: Boolean)

    fun release()

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
    fun removeAnimCache(animIds: Collection<Long>, removeDisplayItem: Boolean = false, clearLayoutPointCache: Boolean = false) {
        animIds.forEach { removeAnimCache(it, removeDisplayItem, false) }
        if (clearLayoutPointCache) {
            AnimCache.clearPointLayoutCache()
        }
    }
}