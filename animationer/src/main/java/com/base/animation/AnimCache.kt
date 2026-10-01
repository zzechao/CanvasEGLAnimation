package com.base.animation

import android.graphics.Point
import com.base.animation.cache.DisplayItemCache
import java.util.concurrent.ConcurrentHashMap

/**
 * @author:zhouzechao
 * description：*
 */
object AnimCache {
    /**
     * 画布缓存
     */
    val displayItemCache = DisplayItemCache()

    /**
     * 坐标记录
     */
    val pointLayoutIDCache = ConcurrentHashMap<Int, Point>()

    fun clear() {
        displayItemCache.clear()
        pointLayoutIDCache.clear()
    }
}