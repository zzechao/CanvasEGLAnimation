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

    /**
     * 清理 View 布局坐标缓存（可指定具体 layoutId，若为 null 则清空全部）
     */
    fun clearPointLayoutCache(idLayout: Int? = null) {
        if (idLayout == null) {
            pointLayoutIDCache.clear()
        } else {
            pointLayoutIDCache.remove(idLayout)
        }
    }
}