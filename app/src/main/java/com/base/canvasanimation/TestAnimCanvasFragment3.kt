package com.base.canvasanimation

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.base.animation.BitmapLoader
import com.base.animation.IAnimListener
import com.base.animation.OnAnimItemClick
import com.base.animation.item.BitmapDisplayItem
import com.base.animation.model.AnimDrawObject
import com.base.animation.node.ImageNode
import com.base.animation.xml.AnimDecoder2
import com.base.animation.xml.AnimEncoder
import com.base.animation.xml.buildAnimNode
import com.base.animation.xml.buildString
import com.base.animation.xml.node.coder.InterpolatorEnum
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.android.synthetic.main.fragment_anim_canvas3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * @author:zhouzechao
 * @date: 2020/11/17
 * description：
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class TestAnimCanvasFragment3 : Fragment(), OnAnimItemClick {

    private val xml =
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" + "<anim>\n" + "    <imageNode displaySize=\"80\" url=\"https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png\">\n" + "        <startAnim alpha=\"255\" startIdName=\"\" startL='{\"x\":0.0,\"y\":0.0}' rotation=\"0.0\" scaleX=\"0.5\" scaleY=\"0.5\">\n" + "            <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"1\" endIdName=\"\" endL='{\"x\":680.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "            <txtNode txtColor=\"#ff0000ff\" fontSize=\"40\" txt=\"测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据\">\n" + "                <startAnim alpha=\"255\" startIdName=\"\" startL='{\"x\":680.0,\"y\":40.0}' rotation=\"0.0\" scaleX=\"1.0\" scaleY=\"1.0\">\n" + "                    <endAnim alpha=\"255\" durTime=\"5000\" interpolator=\"1\" endIdName=\"\" endL='{\"x\":0.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"1.0\" scaleY=\"1.0\" url=\"\" />\n" + "                </startAnim>\n" + "            </txtNode>\n" + "            <layoutNode data=\"\" layoutIdName=\"view_test_layout\" versionCode=\"version_1.0\">\n" + "                <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"0\" endIdName=\"\" endL='{\"x\":680.0,\"y\":2967.0}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "            </layoutNode>\n" + "            <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"2\" endIdName=\"\" endL='{\"x\":1400.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "            <layoutNode data=\"\" layoutIdName=\"view_test_layout\" versionCode=\"version_1.0\">\n" + "                <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"0\" endIdName=\"\" endL='{\"x\":680.0,\"y\":40.0}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\" url=\"\" />\n" + "            </layoutNode>\n" + "        </startAnim>\n" + "    </imageNode>\n" + "    <layoutNode data=\"\" layoutIdName=\"view_test_layout\" versionCode=\"version_1.0\">\n" + "        <startAnim alpha=\"255\" startIdName=\"\" startL='{\"x\":680.0,\"y\":40.0}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\">\n" + "            <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"1\" endIdName=\"\" endL='{\"x\":0.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"3.0\" scaleY=\"3.0\" url=\"\" />\n" + "        </startAnim>\n" + "    </layoutNode>\n" + "</anim>\n"

    private val xmlMore =
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" + "<anim>\n" + "    <startAnim alpha=\"255\" displaySize=\"80\" startId=\"0\" startL='{\"x\":0.0,\"y\":0.0}' rotation=\"0.0\" scaleX=\"0.5\" scaleY=\"0.5\" url=\"https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png\">\n" + "        <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"2\" endId=\"0\" endL='{\"x\":680.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "        <endContainer displaySize=\"0\" durTime=\"1500\" url=\"\">\n" + "            <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":680.0,\"y\":0.0}' rotation=\"360.0\" scaleX=\"1.0\" scaleY=\"1.0\" url=\"\" />\n" + "            <endAnim alpha=\"0\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":0.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"1.0\" scaleY=\"1.0\" url=\"\" />\n" + "            <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":680.0,\"y\":3007.0}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\" url=\"\" />\n" + "            <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":1440.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\" url=\"\" />\n" + "        </endContainer>\n" + "    </startAnim>\n" + "</anim>"

    val red by lazy {
        BitmapLoader.decodeBitmapFrom(resources, R.mipmap.red, 1, 300, 300)
    }

    private val rewardUrl = "https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png"
    private var rewardBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_anim_canvas3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this).asBitmap().load(rewardUrl).override(200, 200).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                rewardBitmap = resource
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
        val ids = mutableSetOf<Long>()
        anim_surface.addAnimListener(object : IAnimListener {
            override fun onStartAnim(animId: Long, extra: String) {
                if (extra == "rain") {
                    if (ids.isEmpty()) {
                        view.post {
                            hide(true)
                        }
                    }
                    ids.add(animId)
                }
            }

            override fun onRunningAnim(animId: Long, extra: String) {
            }

            override fun onCancelAnim(animId: Long, extra: String) {
                if (extra == "rain") {
                    ids.remove(animId)
                    if (ids.isEmpty()) {
                        view.post {
                            hide(false)
                        }
                    }
                }
            }

            override fun onEndAnim(animId: Long, extra: String) {
                if (extra == "rain") {
                    ids.remove(animId)
                    if (ids.isEmpty()) {
                        view.post {
                            hide(false)
                        }
                    }
                }
            }
        })

        anim_1?.setOnClickListener {
            startImageDouAnim2()
            startSingleAnim2()
        }

        anim_2?.setOnClickListener {
            lifecycleScope.launch {
                repeat(100) {
                    startMoreAnim2()
                    delay(50)
                }
            }
        }

        anim_3?.setOnClickListener {
            startAnimRain()
        }
        anim_surface?.setOnItemClick(this)
    }

    override fun onResume() {
        super.onResume()
        anim_surface?.resume()
    }

    override fun onPause() {
        super.onPause()
        anim_surface?.pause()
    }

    private fun startSingleAnim2() {
        val size = 80
        val url = "https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png"
        AnimEncoder().buildAnimNode {
            imageNode {
                this.url = url
                this.displayHeightSize = size
                startNode {
                    point = PointF(0f, 0f)
                    scaleX = 0.5f
                    scaleY = 0.5f
                    endNode {
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                        )
                        rotation = 360f
                        scaleX = 2f
                        scaleY = 2f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                    txtNode {
                        this.txt = "测试数据"
                        this.color = Color.BLUE
                        this.fontSize = 20
                        startNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, size / 2f
                            )
                            scaleX = 1f
                            scaleY = 1f
                            endNode {
                                point = PointF(
                                    0f, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                                )
                                rotation = 360f
                                scaleX = 3f
                                scaleY = 3f
                                durTime = 2000
                                interpolator = InterpolatorEnum.Accelerate.type
                            }

                        }
                    }
                    layoutNode {
                        this.layoutIdName = "view_test_layout"
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, size / 2f
                            )
                            scaleX = 1f
                            scaleY = 1f
                            durTime = 2000
                            interpolator = InterpolatorEnum.Accelerate.type
                        }
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() - size / 2
                            )
                            scaleX = 2f
                            scaleY = 2f
                            durTime = 1000
                            interpolator = InterpolatorEnum.Linear.type
                        }
                    }
                    endNode {
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() - size / 2, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                        )
                        scaleX = 2f
                        scaleY = 2f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Decelerate.type
                    }
                    layoutNode {
                        this.layoutIdName = "view_test_layout"
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, size / 2f
                            )
                            scaleX = 0f
                            scaleY = 0f
                            durTime = 1000
                            interpolator = InterpolatorEnum.Linear.type
                        }
                    }
                }
            }
            layoutNode {
                this.layoutIdName = "view_test_layout"
                startNode {
                    point = PointF(
                        DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, size / 2f
                    )
                    scaleX = 0f
                    scaleY = 0f
                    endNode {
                        point = PointF(
                            0f, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                        )
                        scaleX = 3f
                        scaleY = 3f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                }
            }
        }.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                anim_surface ?: return@launch
                val bitmap = BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    anim_surface,
                    this@apply,
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = bitmap
                        }
                    }
                    displayItem
                }
                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    anim_surface_2, this@apply,
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = bitmap
                        }
                    }
                    displayItem
                }
            }
        }
    }


    private fun startSingleAnim3() {
        lifecycleScope.launch(Dispatchers.IO) {
            AnimDecoder2.suspendPlayAnimWithXml(anim_surface, xml) { node, displayItem ->
                when (displayItem) {
                    is BitmapDisplayItem -> {
                        displayItem.mBitmap = BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
                    }
                }
                displayItem
            }
        }
    }

    private fun startMoreAnim2() {
        val size = 80
        val url = "https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png"
        AnimEncoder().buildAnimNode {
            imageNode {
                this.url = url
                this.displayHeightSize = size
                startNode {
                    point = PointF(0f, 0f)
                    scaleX = 0f
                    scaleY = 0f
                    endNode {
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                        )
                        scaleX = 2f
                        scaleY = 2f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Decelerate.type
                    }
                    endContainer {
                        durTime = 1000
                        endNode {
                            durTime = 500
                            rotation = 360f
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, 0f
                            )
                        }
                        endNode {
                            point = PointF(
                                0f, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                            )
                            alpha = 0
                        }
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat()
                            )
                            scaleX = 0f
                            scaleY = 0f
                        }
                        layoutNode {
                            this.layoutIdName = "view_test_layout"
                            endNode {
                                point = PointF(
                                    DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context).toFloat(), DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context).toFloat() / 2 - size / 2
                                )
                                durTime = 500
                                alpha = 0
                                rotation = 360f
                                scaleX = 2f
                                scaleY = 2f
                            }
                        }
                    }
                }
            }
        }.apply {
            lifecycleScope.launch {
                anim_surface ?: return@launch
                val bitmap = suspendCancellableCoroutine<Bitmap> {
                    Glide.with(this@TestAnimCanvasFragment3).asBitmap().load(url).into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            it.resume(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
                }


                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    anim_surface, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = bitmap
                        }
                    }
                    displayItem
                }

                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    anim_surface_2, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = bitmap
                        }
                    }
                    displayItem
                }
            }
        }
    }

    /**
     * 动画雨
     * 随机排期与 Node 构建放到 Default 线程计算，主线程仅负责读取屏幕尺寸与发起播放，
     * 避免点击瞬间在主线程同步构建 20 个节点（Trace 实测 ~70ms）造成掉帧。
     */
    private fun startAnimRain() {
        val size = 300
        val screenWidth = DisplayUtils.getScreenWidth(this.activity)
        val height = DisplayUtils.getScreenHeight(this.activity).toFloat()
        val indexSize = 20
        // 预热：red 为 lazy 解码，提前在后台触发，避免第一个红包在主线程解码
        lifecycleScope.launch(Dispatchers.Default) {
            val bitmap = red
            val checkOverlapping = mutableMapOf<Long, Int>()
            val plans = ArrayList<Pair<Long, com.base.animation.node.AnimNode>>(indexSize)
            com.base.animation.gles.utils.traceSection("Rain_buildPlans") {
                for (i in 0 until indexSize) {
                    val locationX = getLocationX(screenWidth, size, 3000L, 0L, checkOverlapping)
                    checkOverlapping[locationX.delayTime] = locationX.itemLocationX
                    val node = AnimEncoder().buildAnimNode {
                        imageNode {
                            this.url = "${R.mipmap.red}"
                            this.displayHeightSize = size
                            this.clickable = true
                            this.extras = "rain"
                            startNode {
                                point = PointF(locationX.itemLocationX.toFloat(), 0f)
                                scaleX = 1f
                                scaleY = 1f
                                endNode {
                                    point = PointF(locationX.itemLocationX.toFloat(), height)
                                    scaleX = 1f
                                    scaleY = 1f
                                    durTime = 2000L
                                    interpolator = InterpolatorEnum.Linear.type
                                }
                            }
                        }
                    }
                    plans.add(locationX.delayTime to node)
                }
            }
            // 回到主线程发起各自延时播放（与原逻辑一致）
            withContext(Dispatchers.Main) {
                com.base.animation.gles.utils.traceSection("Rain_startAnimRain") {
                    for ((delayTime, node) in plans) {
                        lifecycleScope.launch {
                            delay(delayTime)
                            anim_surface ?: return@launch
                            AnimDecoder2.suspendPlayAnimWithAnimNode(anim_surface, node) { _, displayItem ->
                                when (displayItem) {
                                    is BitmapDisplayItem -> {
                                        displayItem.mBitmap = bitmap
                                    }
                                }
                                displayItem
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getLocationX(width: Int, size: Int, totalTime: Long, duringTime: Long, checkOverlapping: MutableMap<Long, Int>): LocationX {
        var time = (0L..totalTime).random()
        var itemLocationX = (size / 2..(width - size / 2)).random()
        var timeList = checkOverlapping.filter { abs(it.key - time) < duringTime }
        while (timeList.size > 2) {
            time = (0L..totalTime).random()
            timeList = checkOverlapping.filter { abs(it.key - time) < duringTime }
        }
        var itemList = timeList.filter { abs(itemLocationX - it.value) < size / 2 }
        var i = 0
        while (itemList.isNotEmpty()) {
            itemLocationX = (size / 2..(width - size / 2)).random()
            itemList = timeList.filter { abs(itemLocationX - it.value) < size / 2 }
            i++
            if (i > 5) {
                return getLocationX(width, size, totalTime, duringTime, checkOverlapping)
            }
        }

        return LocationX().apply {
            this.itemLocationX = itemLocationX
            this.delayTime = time
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        anim_surface?.removeAnimListener(null)
    }

    override fun itemClick(animId: Long, animDrawObject: AnimDrawObject, touchPointF: PointF, itemCenterPointF: PointF, extra: String) {
        com.base.animation.gles.utils.traceSection("Rain_itemClick") {
            anim_surface?.setAnimVisible(animId, false)
            val size = 200
            val width = DisplayUtils.getScreenWidth(this.activity).toFloat()
            val height = DisplayUtils.getScreenHeight(this.activity).toFloat()
            val node = AnimEncoder().buildAnimNode {
                imageBezierNode {
                    this.url = rewardUrl
                    this.displayHeightSize = size
                    startNode {
                        point = itemCenterPointF
                        scaleX = 1f
                        scaleY = 1f
                        endNode {
                            point = PointF(width / 2f, height)
                            scaleX = 1f
                            scaleY = 1f
                            durTime = 800L
                            interpolator = InterpolatorEnum.Linear.type
                        }
                    }
                }
            }
            val targetBitmap = rewardBitmap ?: red
            lifecycleScope.launch {
                anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(anim_surface, node) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = targetBitmap
                        }
                    }
                    displayItem
                }
            }
        }
    }

    inner class LocationX {
        var itemLocationX: Int = -1
        var delayTime: Long = 0L
    }

    private fun hide(isHide: Boolean) {
        if (isHide) {
            anim_1.visibility = View.GONE
            anim_2.visibility = View.GONE
            anim_3.visibility = View.GONE
        } else {
            anim_1.visibility = View.VISIBLE
            anim_2.visibility = View.VISIBLE
            anim_3.visibility = View.VISIBLE
        }
    }

    private fun startImageDouAnim2() {
        val size = resources.getDimensionPixelSize(R.dimen.size)
        val url = "http://imgs.pago.tv/gifts/69753013-38d8-44de-8a14-286cf4f81083.png"
        AnimEncoder().buildAnimNode {
            imageDouNode {
                this.rocation = 10
                this.url = url
                this.displayHeightSize = size
                startNode {
                    scaleX = 1f
                    scaleY = 1f
                    point = PointF(
                        DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context) / 2f, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context) / 1f
                    )
                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context) / 2f, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context) / 2f
                        )
                        durTime = 1000
                        interpolator = InterpolatorEnum.Decelerate.type
                    }
                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment3.context) / 2f, DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment3.context) / 1f
                        )
                        durTime = 2000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                }
            }
        }.buildString().apply {
            lifecycleScope.launch {
                anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithXml(
                    anim_surface, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is ImageDouNode.BitmapDouDisplay -> {
                            loadImage(fragment = this@TestAnimCanvasFragment3, (node as ImageNode).url, node.displayHeightSize)?.let {
                                displayItem.setBitmap(it)
                            }
                        }

                        is BitmapDisplayItem -> {
                            displayItem.setBitmap(
                                BitmapLoader.decodeBitmapFrom(
                                    resources, R.mipmap.xin, 1, 100, 100
                                )
                            )
                        }
                    }
                    displayItem
                }
                AnimDecoder2.suspendPlayAnimWithXml(
                    anim_surface_2, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is ImageDouNode.BitmapDouDisplay -> {
                            loadImage(fragment = this@TestAnimCanvasFragment3, (node as ImageNode).url, node.displayHeightSize)?.let {
                                displayItem.setBitmap(it)
                            }
                        }

                        is BitmapDisplayItem -> {
                            displayItem.setBitmap(
                                BitmapLoader.decodeBitmapFrom(
                                    resources, R.mipmap.xin, 1, 100, 100
                                )
                            )
                        }
                    }
                    displayItem
                }
            }
        }
    }
}