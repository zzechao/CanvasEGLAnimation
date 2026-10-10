package com.base.animation.helper

import android.graphics.PointF
import android.util.Log
import com.base.animation.AnimCache
import com.base.animation.Animer
import com.base.animation.IAnimListener
import com.base.animation.OnAnimItemClick
import com.base.animation.fpsTime
import com.base.animation.helper.data.PathProcess
import com.base.animation.helper.data.PathProcessItem
import com.base.animation.item.BaseDisplayItem
import com.base.animation.model.AnimDrawObject
import com.base.animation.model.AnimPathObject
import android.util.LruCache
import com.base.animation.model.BaseAnimDrawObject
import com.base.animation.model.DrawObject
import com.base.animation.model.toAnimDrawObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author:zhouzechao
 * @date: 2020/12/9
 * description：处理animPathObject转每一帧的绘制点
 */
class PathObjectDeal(private val parserEnd: () -> Unit) : IPathObjectDeal {
    companion object {
        private const val TAG = "PathObjectDeal"
    }

    private var animDisplayScope: CoroutineScope? = null
    private var animPather: SendChannel<AnimPathObject>? = null

    private val md by lazy { MessageDigest.getInstance("MD5") }

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
     * 路径缓存
     */
    private val pathCacheMap: LruCache<String, MutableMap<Int, MutableList<AnimDrawObject>>> =
        LruCache(50)

    /**
     * animId 关联的 pathKey 映射（用于按 animId 动态精准作废轨迹缓存）
     */
    private val animIdPathKeys = ConcurrentHashMap<Long, MutableSet<String>>()

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
            Animer.exceptionHandler, capacity = 300
        ) {
            supervisorScope {
                for (animPath in this@actor) {
                    if (!isActive) break
                    val currentAnimId = animPath.animId
                    if (animPath.displayItemsMap.isNotEmpty()) {
                        AnimCache.displayItemCache.putDisplayItems(animPath.displayItemsMap)
                        animIdDisplayItemKeys.getOrPut(currentAnimId) { CopyOnWriteArraySet() }
                            .addAll(animPath.displayItemsMap.keys)
                    }
                var drawsMap = mutableMapOf<Int, MutableList<AnimDrawObject>>()
                var position = 0
                val drawObject = DrawObject(animPath.animId, animPath.expand)
                for ((index, starts) in animPath.startPoints) {
                    var startPosition = position
                    if (starts.isNotEmpty()) {
                        animPath.animPathMap[index]?.apply {
                            this.forEachIndexed { index, pathObjectWithDer ->
                                val duringTime = pathObjectWithDer.during
                                val times = duringTime * 1f / fpsTime
                                val start = starts.getOrNull(index) ?: return@forEachIndexed

                                val end = pathObjectWithDer.pathObject
                                end.getItem(start)

                                val displayItem = animPath.displayItemsMap[start.displayItemId] ?: getDisplayItem(start.displayItemId)
                                var pathKey = "${fpsTime}_${start.key()}_${end.key()}_${duringTime}_${displayItem?.let { "${it::class.simpleName}_${it.isCalculate}" }}"
                                pathKey = md.digest(pathKey.toByteArray(Charsets.UTF_8)).toHexString()

                                animIdPathKeys.getOrPut(currentAnimId) { CopyOnWriteArraySet() }.add(pathKey)
                                animIdDisplayItemKeys.getOrPut(currentAnimId) { CopyOnWriteArraySet() }.add(start.displayItemId)

                                drawsMap = pathCacheMap.get(pathKey) ?: run {
                                    val pathProcessItem = PathProcessItem(
                                        end.itemX, end.itemY, end.itemAlpha, end.itemScaleX, end.itemScaleY, end.itemRotation
                                    )
                                    val pathProcess = PathProcess(
                                        start.toAnimDrawObject(animPath.clickable, animPath.expand), end.toAnimDrawObject(animPath.clickable, animPath.expand), start.interpolator, duringTime, item = pathProcessItem, current = start.toAnimDrawObject(animPath.clickable, animPath.expand), clickable = animPath.clickable, extra = animPath.expand
                                    )

                                    if (drawsMap[startPosition] == null) {
                                        drawsMap[startPosition] = mutableListOf()
                                    }
                                    drawsMap[startPosition]?.add(
                                        start.toAnimDrawObject(animPath.clickable, animPath.expand)
                                    )

                                    for (i in 0..times.toInt()) {
                                        startPosition++
                                        val p = if (duringTime <= 0L) 1f else (i * fpsTime * 1f / duringTime).coerceIn(0f, 1f)
                                        if (displayItem?.isCalculate == true) {
                                            pathProcess.curTotalTime += fpsTime
                                            val animDrawObject = AnimDrawObject(start.displayItemId)
                                            displayItem.calculate(pathProcess, animDrawObject, start.interpolator)
                                            if (drawsMap[startPosition] == null) {
                                                drawsMap[startPosition] = mutableListOf()
                                            }
                                            drawsMap[startPosition]?.add(animDrawObject)
                                        } else {
                                            val interP = start.interpolator.getInterpolation(p)
                                            val inPoint = PointF(
                                                start.point.x + end.itemX * interP, start.point.y + end.itemY * interP
                                            )
                                            val animDrawObject = AnimDrawObject(
                                                start.displayItemId, clickable = animPath.clickable, expand = animPath.expand
                                            ).apply {
                                                point = inPoint
                                                alpha = start.alpha + (end.itemAlpha * interP).toInt()
                                                scaleX = start.scaleX + end.itemScaleX * interP
                                                scaleY = start.scaleY + end.itemScaleY * interP
                                                rotation = start.rotation + end.itemRotation * interP
                                            }
                                            if (drawsMap[startPosition] == null) {
                                                drawsMap[startPosition] = mutableListOf()
                                            }
                                            drawsMap[startPosition]?.add(animDrawObject)
                                        }

                                    }
                                    // 当索引为这轮终点的长度时，如果是进入下一轮计算，否则继续执行这一轮的计算，从这轮的那个position开始计算
                                    if (index == this.size - 1) {
                                        position = startPosition
                                        position++
                                    } else {
                                        startPosition = position
                                    }
                                    pathCacheMap.put(pathKey, drawsMap)
                                    drawsMap
                                }
                            }
                        }
                    }
                }
                drawObject.animDraws = drawsMap
                animDrawObjects[drawObject.animId] = drawObject
                animDrawIds.add(drawObject.animId)
                parserEnd()
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
        // 1. 移除并作废路径轨迹缓存
        val pathKeys = animIdPathKeys.remove(animId)
        pathKeys?.forEach { key ->
            pathCacheMap.remove(key)
        }

        // 2. 如果指定要移除对应的 DisplayItem（图片/文本等），在无其他活跃动画引用时安全移除
        val displayKeys = animIdDisplayItemKeys.remove(animId)
        if (removeDisplayItem && displayKeys != null) {
            displayKeys.forEach { displayId ->
                val isReferencedByOther = animIdDisplayItemKeys.values.any { it.contains(displayId) }
                if (!isReferencedByOther) {
                    AnimCache.displayItemCache.removeDisplayItem(displayId)
                }
            }
        }

        // 3. 及时清理 View 布局坐标缓存
        if (clearLayoutPointCache) {
            AnimCache.clearPointLayoutCache()
        }

        // 4. 清理内存中关联对象
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
        pathCacheMap.evictAll()
        animIdPathKeys.clear()
        animIdDisplayItemKeys.clear()
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuffer()
        for (b in this) {
            val i = b.toInt() and 0xff
            var hexString = Integer.toHexString(i)
            if (hexString.length < 2) {
                hexString = "0$hexString"
            }
            sb.append(hexString)
        }
        return sb.toString()
    }
}


