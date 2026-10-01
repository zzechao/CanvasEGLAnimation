package com.base.animation.xml

import com.base.animation.AnimationEx
import com.base.animation.DisplayObject
import com.base.animation.IAnimView
import com.base.animation.getFragmentActivity
import com.base.animation.item.BaseDisplayItem
import com.base.animation.item.BitmapDisplayItem
import com.base.animation.item.LayoutDisplayItem
import com.base.animation.item.StringDisplayItem
import com.base.animation.model.AnimPathObject
import com.base.animation.node.AnimNode
import com.base.animation.node.EndNode
import com.base.animation.node.EndNodeContainer
import com.base.animation.node.IAnimNode
import com.base.animation.node.IXmlDrawableNode
import com.base.animation.node.IXmlDrawableNodeDealIntercept
import com.base.animation.node.ImageNode
import com.base.animation.node.LayoutNode
import com.base.animation.node.StartNode
import com.base.animation.node.TextNode
import com.base.animation.tryCatch
import com.base.animation.xml.node.AnimNodeChain
import com.base.animation.xml.node.coder.IAttributeCoder


typealias DealDisplayItem = suspend (
    IXmlDrawableNode, BaseDisplayItem
) -> BaseDisplayItem

typealias IDealNodeDealIntercept = suspend (
    displayObject: DisplayObject, animNode: IAnimNode, chain: AnimNodeChain, dealDisplayItem: DealDisplayItem
) -> String

object AnimDecoder2 {

    internal val decoder by lazy {
        XmlObjectDecoder().apply {
            registerNodeCreatetor(AnimNode::class.java)
            registerNodeCreatetor(StartNode::class.java)
            registerNodeCreatetor(EndNode::class.java)
            registerNodeCreatetor(EndNodeContainer::class.java)
            registerNodeCreatetor(ImageNode::class.java)
            registerNodeCreatetor(TextNode::class.java)
            registerNodeCreatetor(LayoutNode::class.java)
        }
    }

    val mapNodeAttributeCoderMap by lazy {
        mutableMapOf<String, IAttributeCoder<out Any>>()
    }

    suspend fun suspendPlayAnimWithAnimNode(
        anim: IAnimView, animNode: AnimNode, dealDisplayItem: DealDisplayItem
    ): List<Long> {
        return dealAnim(anim, animNode, dealDisplayItem)
    }

    private suspend fun dealAnim(
        anim: IAnimView, animNode: AnimNode, dealDisplayItem: DealDisplayItem
    ): List<Long> {
        val listAnimIds = mutableListOf<Long>()
        val displayObject = DisplayObject.with()
        animNode.getNodes().forEach {
            val chain = AnimNodeChain(anim)
            val path = AnimPathObject.Inner.with()
            chain.path = path
            dealAnim(displayObject, it, chain, dealDisplayItem)
            var clickable = false
            var extra = ""
            if (it is IXmlDrawableNode) {
                clickable = it.clickable
                extra = it.extras
            }
            val data = displayObject.build()
            chain.anim.addAnimDisplay(path.build(data).apply {
                this.clickable = clickable
                this.expand = extra
                listAnimIds.add(this.animId)
            })
        }
        return listAnimIds
    }

    private suspend fun dealAnim(
        displayObject: DisplayObject,
        animNode: IAnimNode,
        chain: AnimNodeChain,
        dealDisplayItem: DealDisplayItem,
        isContainer: Boolean = false
    ) {
        when (animNode) {
            is IXmlDrawableNodeDealIntercept -> {
                if (animNode is IXmlDrawableNode) {
                    if (animNode.getNodes().isEmpty()) return
                    val displayId = animNode.dealIntercept.invoke(
                        displayObject, animNode, chain, dealDisplayItem
                    )
                    if (displayId.isNotEmpty()) {
                        chain.curDisplayId = displayId
                    }
                    if (isContainer) return
                    animNode.getNodes().forEach { // 过滤只处理StartNode、EndNode、EndNodeContainer
                        if (it is EndNode || it is EndNodeContainer || it is StartNode) {
                            dealAnim(displayObject, it, chain, dealDisplayItem)
                        }
                    }
                }
            }

            is ImageNode -> {
                if (animNode.getNodes().isEmpty()) return
                val key = animNode.url + animNode.displayHeightSize + animNode.nodeName
                val displayId = displayObject.suspendAdd(
                    key = key, kClass = animNode.displayItem
                ) {
                    val bitmapDisplayItem = BitmapDisplayItem()
                    dealDisplayItem.invoke(animNode, bitmapDisplayItem) // 代理出去处理图片的加载方式
                    val bitmapWidth = bitmapDisplayItem.mBitmap?.width ?: return@suspendAdd null
                    val bitmapHeight = bitmapDisplayItem.mBitmap?.height ?: return@suspendAdd null
                    if (bitmapHeight <= 0) return@suspendAdd null
                    val displayWidth = animNode.displayHeightSize * bitmapWidth / bitmapHeight
                    bitmapDisplayItem.setDisplaySize(displayWidth, animNode.displayHeightSize)
                    bitmapDisplayItem
                }
                if (displayId.isNotEmpty()) {
                    chain.curDisplayId = displayId
                }
                if (isContainer) return
                animNode.getNodes().forEach {
                    if (it is EndNode || it is EndNodeContainer || it is StartNode) {
                        dealAnim(displayObject, it, chain, dealDisplayItem)
                    }
                }
            }

            is TextNode -> {
                if (animNode.getNodes().isEmpty()) return
                val key = animNode.txt + animNode.fontSize + animNode.color + animNode.nodeName
                val displayId = displayObject.suspendAdd(
                    key = key, kClass = animNode.displayItem
                ) {
                    val stringDisplayItem = StringDisplayItem(
                        animNode.fontSize, animNode.txt, animNode.color, animNode.maxWidth, animNode.singleLine
                    )
                    dealDisplayItem.invoke(
                        animNode, stringDisplayItem
                    )
                }
                if (displayId.isNotEmpty()) {
                    chain.curDisplayId = displayId
                }
                if (isContainer) return
                animNode.getNodes().forEach {
                    if (it is EndNode || it is EndNodeContainer || it is StartNode) {
                        dealAnim(displayObject, it, chain, dealDisplayItem)
                    }
                }
            }

            is LayoutNode -> {
                if (animNode.getNodes().isEmpty()) return
                val key = animNode.layoutIdName + animNode.data
                val displayId = displayObject.suspendAdd(
                    key = key, kClass = animNode.displayItem
                ) {
                    val layoutId = tryCatch {
                        val context = AnimationEx.mApplication ?: return@tryCatch 0
                        context.resources?.getIdentifier(
                            animNode.layoutIdName, "layout", context.packageName
                        )
                    } ?: 0
                    if (layoutId > 0) {
                        val viewContext = chain.anim.getView()?.context ?: AnimationEx.mApplication ?: return@suspendAdd null
                        val layoutDisplayItem = LayoutDisplayItem(viewContext, layoutId)
                        dealDisplayItem.invoke(
                            animNode, layoutDisplayItem
                        )
                        return@suspendAdd layoutDisplayItem
                    } else {
                        return@suspendAdd null
                    }
                }
                if (displayId.isNotEmpty()) {
                    chain.curDisplayId = displayId
                }
                if (isContainer) return
                animNode.getNodes().forEach {
                    if (it is EndNode || it is EndNodeContainer || it is StartNode) {
                        dealAnim(displayObject, it, chain, dealDisplayItem)
                    }
                }
            }

            is StartNode -> {
                if (animNode.point != null && animNode.getNodes().isNotEmpty()) {
                    val start = animNode.decode(chain.curDisplayId, chain.anim)
                    if (chain.isBeginPointSet) {
                        chain.nextStart = start
                    } else {
                        chain.start = start
                    }
                    val displayObjectId = chain.curDisplayId
                    animNode.getNodes().forEach {
                        when (it) {
                            is EndNode -> {
                                chain.curDisplayId = displayObjectId
                                dealAnim(displayObject, it, chain, dealDisplayItem)
                            }

                            is EndNodeContainer, is IXmlDrawableNode -> {
                                dealAnim(displayObject, it, chain, dealDisplayItem)
                            }
                        }
                    }
                }
            }

            is EndNode -> {
                val next = animNode.decode(chain.curDisplayId, chain.anim)
                chain.buildAnim(next, animNode.durTime)
            }

            is EndNodeContainer -> {
                val displayId = chain.curDisplayId
                animNode.getNodes().forEach {
                    when (it) {
                        is EndNode -> {
                            val next = it.decode(displayId, chain.anim)
                            val durTime = if (it.durTime == 0L) {
                                animNode.durTime
                            } else {
                                it.durTime
                            }
                            chain.recordAnimContainer(next, durTime)
                        }

                        is IXmlDrawableNode -> {
                            dealAnim(displayObject, it, chain, dealDisplayItem, true)
                            it.getNodes().firstOrNull()?.let {
                                if (it is EndNode) {
                                    val next = it.decode(chain.curDisplayId, chain.anim)
                                    val durTime = if (it.durTime == 0L) {
                                        animNode.durTime
                                    } else {
                                        it.durTime
                                    }
                                    chain.recordAnimContainer(next, durTime)
                                }
                            }
                        }
                    }
                }
                chain.buildAnimContainer()
            }
        }
    }

    suspend fun suspendPlayAnimWithXml(
        anim: IAnimView, xml: String, dealDisplayItem: DealDisplayItem
    ): List<Long> {
        return dealStarAnimXml(anim, xml, dealDisplayItem)
    }

    private suspend fun dealStarAnimXml(
        anim: IAnimView, xml: String, dealDisplayItem: DealDisplayItem
    ): List<Long> {
        (decoder.createObject(xml) as? AnimNode)?.let {
            return dealAnim(anim, it, dealDisplayItem)
        }
        return emptyList()
    }
}