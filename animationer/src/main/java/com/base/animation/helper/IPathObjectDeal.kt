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
}