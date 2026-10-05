package com.base.animation.gles

import com.base.animation.Animer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author zzechao
 * @date 2026/10/2
 * @desc EGL渲染工作组动态扩容分配器
 * 支持动态配置每组支持的EGLView上限个数（如默认4个，也可配置6、8个等）。
 * 当组内View数量未满时复用已有线程与Context，满额后自动开辟新的工作组线程与Context；空闲时自动释放。
 */
object EGLGroupManager {

    private const val TAG = "EGLGroupManager"

    /**
     * 每个渲染组最大承载的 View 数量（支持动态配置）
     * 默认 4 个（每个 View 独享一个后台 GL 线程，多核 CPU 算力完全释放，双 View / 多 View 并发时 0 排队等待；
     * 同时各组通过 Root SharedContext 100% 共享共享纹理池显存；若需要收敛线程数，可随时动态调大为 2、4 等）
     */
    @Volatile
    var maxViewsPerGroup: Int = 4

    private val groupCounter = AtomicInteger(0)
    private val activeGroups = CopyOnWriteArrayList<EGLRenderGroup>()

    /**
     * 获取或分配一个渲染组
     */
    @Synchronized
    fun obtainGroup(): EGLRenderGroup {
        val limit = maxViewsPerGroup.coerceAtLeast(1)

        // 寻找现有未满额且未释放的组
        for (group in activeGroups) {
            if (!group.isReleased.get() && group.viewCount.get() < limit) {
                val current = group.viewCount.incrementAndGet()
                Animer.log.i(TAG, "obtainGroup: assigned to Group-${group.id} (views: $current/$limit)")
                return group
            }
        }

        // 所有组均已满额，自动扩容开辟新组
        val newId = groupCounter.incrementAndGet()
        val newGroup = EGLRenderGroup(newId)
        newGroup.viewCount.incrementAndGet()
        activeGroups.add(newGroup)
        Animer.log.i(TAG, "obtainGroup: scale up -> created Group-${newGroup.id} (views: 1/$limit, total active groups: ${activeGroups.size})")
        return newGroup
    }

    /**
     * 归还/注销一个 View 槽位
     */
    @Synchronized
    fun releaseGroup(group: EGLRenderGroup) {
        val remaining = group.viewCount.decrementAndGet()
        Animer.log.i(TAG, "releaseGroup: Group-${group.id} remaining views: $remaining")
        if (remaining <= 0) {
            group.viewCount.set(0)
            activeGroups.remove(group)
            group.releaseGroup()
            Animer.log.i(TAG, "releaseGroup: Group-${group.id} idle released (active groups left: ${activeGroups.size})")
        }
    }

    /**
     * 当前运行中的工作组数量
     */
    fun getActiveGroupCount(): Int = activeGroups.size

    /**
     * 当前所有组承载的 View 总数
     */
    fun getTotalActiveViewCount(): Int = activeGroups.sumOf { it.viewCount.get() }

    /**
     * 重置所有工作组（页面销毁或测试复位时调用）
     */
    @Synchronized
    fun clearAll() {
        activeGroups.forEach { it.releaseGroup() }
        activeGroups.clear()
    }
}
