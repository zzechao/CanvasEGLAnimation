package com.base.animation.cache

import android.util.LruCache
import com.base.animation.Animer
import com.base.animation.item.BaseDisplayItem

/**
 * @author:zhouzechao
 * @date: 1/22/21
 * description：DisplayItem缓存
 */

private const val TAG = "DisplayItemCache"

class DisplayItemCache {

    var displayMaxCacheSize: Long = 200L

    private val caches: LruCache<String, BaseDisplayItem> by lazy {
        LruCache(displayMaxCacheSize.toInt())
    }

    fun putDisplayItems(displayItems: MutableMap<String, out BaseDisplayItem>) {
        Animer.log.i(TAG, "putDisplayItems displayItems:${displayItems.size}")
        displayItems.forEach {
            caches.put(it.key, it.value)
        }
    }

    /**
     * 获取
     */
    fun getDisplayItem(displayItemId: String): BaseDisplayItem? {
        return caches.get(displayItemId)
    }

    /**
     * 是否含有对应的key和clazz
     */
    fun hasDisplayItem(key: String): Boolean {
        return caches.get(key) != null
    }

    /**
     * 清空一级和二级缓存
     */
    fun clear() {
        caches.evictAll()
    }
}