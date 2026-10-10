package com.base.animation.helper

import android.graphics.PointF
import com.base.animation.AnimCache
import com.base.animation.Animer
import com.base.animation.IAnimListener
import com.base.animation.OnAnimItemClick
import com.base.animation.helper.data.PathProcess
import com.base.animation.helper.data.PathProcessItem
import com.base.animation.item.BaseDisplayItem
import com.base.animation.model.AnimPathObject
import com.base.animation.model.BaseAnimDrawObject
import com.base.animation.model.DrawObject2
import com.base.animation.model.toAnimDrawObject2
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author:zhouzechao
 * description：*
 */
class PathObjectDeal2(private val parserEnd: () -> Unit) : IPathObjectDeal {

    private var animDisplayScope: CoroutineScope? = null
    private var animPather: SendChannel<AnimPathObject>? = null

    /**
     * 路径坐标
     */
    override val animDrawIds: MutableList<Long> = CopyOnWriteArrayList()
    override val animDrawObjects: ConcurrentHashMap<Long, BaseAnimDrawObject> = ConcurrentHashMap()

    /**
     * 点击事件列表
     */
    override var onItemListener: OnAnimItemClick? = null

    /**
     * 动画事件
     */
    override val animListeners: MutableSet<IAnimListener> = CopyOnWriteArraySet()

    /**
     * animId 关联的 displayItemId 映射（用于按 animId 关联显示项）
     */
    private val animIdDisplayItemKeys = ConcurrentHashMap<Long, MutableSet<String>>()

    init {
        getOrInitPather()
    }

    /**
     * 计算路径上的各个坐标点（按需懒加载/重建，支持在 release 之后重新创建）
     */
    @Synchronized
    private fun getOrInitPather(): SendChannel<AnimPathObject> {
        val currentPather = animPather
        val currentScope = animDisplayScope
        if (currentPather != null && !currentPather.isClosedForSend && currentScope != null && currentScope.isActive) {
            return currentPather
        }
        val scope = CoroutineScope(Animer.calculationDispatcher + SupervisorJob() + Animer.exceptionHandler)
        animDisplayScope = scope
        val pather = scope.actor<AnimPathObject>(
            Animer.exceptionHandler,
            capacity = 300
        ) {
            supervisorScope {
                for (animPath in this@actor) {
                    if (!isActive) break
                    launch(coroutineContext) {
                    val currentAnimId = animPath.animId
                    if (animPath.displayItemsMap.isNotEmpty()) {
                        AnimCache.displayItemCache.putDisplayItems(animPath.displayItemsMap)
                        animIdDisplayItemKeys.getOrPut(currentAnimId) { CopyOnWriteArraySet() }
                            .addAll(animPath.displayItemsMap.keys)
                    }
                    val clickable = animPath.clickable
                    val expand = animPath.expand
                    val drawObject = DrawObject2(animPath.animId, expand)
                    val drawPathProcessMap = mutableMapOf<Int, List<PathProcess>>()
                    for ((index, starts) in animPath.startPoints) {
                        if (starts.isNotEmpty()) {
                            val pathProcesses = animPath.animPathMap[index]?.let {
                                it.mapIndexed { index, pathObjectWithDer ->
                                    val duringTime = pathObjectWithDer.during
                                    val start = starts[index]
                                    animIdDisplayItemKeys.getOrPut(currentAnimId) { CopyOnWriteArraySet() }.add(start.displayItemId)
                                    val startAnimObject = start.toAnimDrawObject2()
                                    val end = pathObjectWithDer.pathObject
                                    val endAnimObject = end.toAnimDrawObject2()
                                    val totalX = endAnimObject.point.x - startAnimObject.point.x
                                    val totalY = endAnimObject.point.y - startAnimObject.point.y
                                    val totalAlpha = endAnimObject.alpha - startAnimObject.alpha
                                    val totalScaleX = endAnimObject.scaleX - startAnimObject.scaleX
                                    val totalScaleY = endAnimObject.scaleY - startAnimObject.scaleY
                                    val totalRotation = endAnimObject.rotation - startAnimObject.rotation
                                    val pathProcessItem = PathProcessItem(totalX, totalY, totalAlpha, totalScaleX, totalScaleY, totalRotation)
                                    val currentAnimObject = startAnimObject.copy(point = PointF(startAnimObject.point.x, startAnimObject.point.y))
                                    PathProcess(
                                        startAnimObject, endAnimObject, start.interpolator, duringTime, 0f,
                                        pathProcessItem, currentAnimObject, clickable, expand
                                    )
                                }
                            }
                            pathProcesses?.let { drawPathProcessMap[index] = it }
                        }
                    }
                    drawObject.animDraws = drawPathProcessMap
                    animDrawObjects[drawObject.animId] = drawObject
                    animDrawIds.add(drawObject.animId)
                    parserEnd()
                }
            }
        }
    }
        animPather = pather
        return pather
    }


    /**
     * 获取显示的displayItem
     */
    override fun getDisplayItem(displayItemId: String): BaseDisplayItem? {
        return AnimCache.displayItemCache.getDisplayItem(displayItemId)
    }

    /**
     * 加入路径，并刷新画布缓存策略的时间
     */
    override fun sendAnimPath(animPathObject: AnimPathObject) {
        getOrInitPather().offer(animPathObject)
    }

    /**
     * 是否有任务
     */
    override fun hasTask(): Boolean {
        return animDrawIds.isNotEmpty()
    }


    /**
     * 清空执行中ids
     */
    override fun removeAnimId(animId: Long, isCancel: Boolean) {
        animDrawIds.remove(animId)
        val animObject = animDrawObjects.remove(animId)
        if (isCancel && animListeners.isNotEmpty()) {
            animListeners.forEach {
                it.onCancelAnim(animId, animObject?.extra ?: "")
            }
        }
    }

    override fun setAnimVisible(animId: Long, isVisible: Boolean) {
        animDrawObjects[animId]?.isVisible = isVisible
    }

    override fun removeAnimCache(animId: Long, removeDisplayItem: Boolean, clearLayoutPointCache: Boolean) {
        val displayKeys = animIdDisplayItemKeys.remove(animId)
        if (removeDisplayItem && displayKeys != null) {
            displayKeys.forEach { displayId ->
                val isReferencedByOther = animIdDisplayItemKeys.values.any { it.contains(displayId) }
                if (!isReferencedByOther) {
                    AnimCache.displayItemCache.removeDisplayItem(displayId)
                }
            }
        }
        if (clearLayoutPointCache) {
            AnimCache.clearPointLayoutCache()
        }
        animDrawObjects.remove(animId)
        animDrawIds.remove(animId)
    }

    override fun release() {
        try {
            animPather?.close()
        } catch (t: Throwable) {
            // ignore
        }
        animPather = null
        animDisplayScope?.cancel()
        animDisplayScope = null
        animDrawIds.clear()
        animDrawObjects.clear()
        animListeners.clear()
        onItemListener = null
        animIdDisplayItemKeys.clear()
    }
}