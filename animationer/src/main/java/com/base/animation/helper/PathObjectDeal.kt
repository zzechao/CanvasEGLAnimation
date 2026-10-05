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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.supervisorScope
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author:zhouzechao
 * @date: 2020/12/9
 * description：处理animPathObject转每一帧的绘制点
 */
class PathObjectDeal(parserEnd: () -> Unit) : IPathObjectDeal {
    companion object {
        private const val TAG = "PathObjectDeal"
    }

    private val animDisplayScope = CoroutineScope(Animer.calculationDispatcher)

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
     * 计算路径上的各个坐标点
     */
    private val animPather = animDisplayScope.actor<AnimPathObject>(
        Animer.exceptionHandler, capacity = 300
    ) {
        supervisorScope {
            for (animPath in this@actor) {
                if (animPath.displayItemsMap.isNotEmpty()) {
                    AnimCache.displayItemCache.putDisplayItems(animPath.displayItemsMap)
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
        animPather.offer(animPathObject)
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


