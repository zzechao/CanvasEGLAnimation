package com.base.canvasanimation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.NinePatch
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.base.animation.Animer
import com.base.animation.BitmapLoader
import com.base.animation.OnAnimItemClick
import com.base.animation.item.BitmapDisplayItem
import com.base.animation.item.LayoutDisplayItem
import com.base.animation.model.AnimDrawObject
import com.base.animation.node.ImageNode
import com.base.animation.xml.AnimDecoder2
import com.base.animation.xml.AnimEncoder
import com.base.animation.xml.buildAnimNode
import com.base.animation.xml.buildString
import com.base.animation.xml.node.coder.InterpolatorEnum
import com.base.canvasanimation.TestAnimCanvasFragment.LocationX
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs


/**
 * @author:zhouzechao
 * @date: 2020/11/17
 * description：
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class TestAnimCanvasFragment : Fragment(), OnAnimItemClick {

    private val xml =
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" + "<anim>\n" + "    <imageNode displaySize=\"80\" url=\"https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png\">\n" + "        <startAnim alpha=\"255\" startIdName=\"\" startL='{\"x\":0.0,\"y\":0.0}' rotation=\"0.0\" scaleX=\"0.5\" scaleY=\"0.5\">\n" + "            <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"1\" endIdName=\"\" endL='{\"x\":680.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "            <txtNode txtColor=\"#ff0000ff\" fontSize=\"40\" txt=\"测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据测试数据\">\n" + "                <startAnim alpha=\"255\" startIdName=\"\" startL='{\"x\":680.0,\"y\":40.0}' rotation=\"0.0\" scaleX=\"1.0\" scaleY=\"1.0\">\n" + "                    <endAnim alpha=\"255\" durTime=\"5000\" interpolator=\"1\" endIdName=\"\" endL='{\"x\":0.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"1.0\" scaleY=\"1.0\" url=\"\" />\n" + "                </startAnim>\n" + "            </txtNode>\n" + "            <layoutNode data=\"\" layoutIdName=\"view_test_layout\" versionCode=\"version_1.0\">\n" + "                <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"0\" endIdName=\"\" endL='{\"x\":680.0,\"y\":2967.0}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "            </layoutNode>\n" + "            <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"2\" endIdName=\"\" endL='{\"x\":1400.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "            <layoutNode data=\"\" layoutIdName=\"view_test_layout\" versionCode=\"version_1.0\">\n" + "                <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"0\" endIdName=\"\" endL='{\"x\":680.0,\"y\":40.0}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\" url=\"\" />\n" + "            </layoutNode>\n" + "        </startAnim>\n" + "    </imageNode>\n" + "    <layoutNode data=\"\" layoutIdName=\"view_test_layout\" versionCode=\"version_1.0\">\n" + "        <startAnim alpha=\"255\" startIdName=\"\" startL='{\"x\":680.0,\"y\":40.0}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\">\n" + "            <endAnim alpha=\"255\" durTime=\"1000\" interpolator=\"1\" endIdName=\"\" endL='{\"x\":0.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"3.0\" scaleY=\"3.0\" url=\"\" />\n" + "        </startAnim>\n" + "    </layoutNode>\n" + "</anim>\n"

    private val xmlMore =
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" + "<anim>\n" + "    <startAnim alpha=\"255\" displaySize=\"80\" startId=\"0\" startL='{\"x\":0.0,\"y\":0.0}' rotation=\"0.0\" scaleX=\"0.5\" scaleY=\"0.5\" url=\"https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png\">\n" + "        <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"2\" endId=\"0\" endL='{\"x\":680.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"2.0\" scaleY=\"2.0\" url=\"\" />\n" + "        <endContainer displaySize=\"0\" durTime=\"1500\" url=\"\">\n" + "            <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":680.0,\"y\":0.0}' rotation=\"360.0\" scaleX=\"1.0\" scaleY=\"1.0\" url=\"\" />\n" + "            <endAnim alpha=\"0\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":0.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"1.0\" scaleY=\"1.0\" url=\"\" />\n" + "            <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":680.0,\"y\":3007.0}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\" url=\"\" />\n" + "            <endAnim alpha=\"255\" displaySize=\"0\" durTime=\"1000\" interpolator=\"0\" endId=\"0\" endL='{\"x\":1440.0,\"y\":1463.5}' rotation=\"0.0\" scaleX=\"0.0\" scaleY=\"0.0\" url=\"\" />\n" + "        </endContainer>\n" + "    </startAnim>\n" + "</anim>"

    val red by lazy {
        BitmapLoader.decodeBitmapFrom(resources, R.mipmap.red, 1, 300, 300)
    }

    private val code = "imageNode {\n" +
            "                this.url = url\n" +
            "                this.displayHeightSize = size\n" +
            "                startNode {\n" +
            "                    layoutIdName = \"test1\"\n" +
            "                    point = PointF(0f, 0f)\n" +
            "                    scaleX = 0.5f\n" +
            "                    scaleY = 0.5f\n" +
            "                    endNode {\n" +
            "                        layoutIdName = \"test2\"\n" +
            "                        point = PointF(500f, 500f)\n" +
            "                        scaleX = 3f\n" +
            "                        scaleY = 3f\n" +
            "                        durTime = 1000\n" +
            "                        interpolator = InterpolatorEnum.Accelerate.type\n" +
            "                    }\n" +
            "                    endNode {\n" +
            "                        layoutIdName = \"test3\"\n" +
            "                        point = PointF(500f, 0f)\n" +
            "                        scaleX = 0.5f\n" +
            "                        scaleY = 0.5f\n" +
            "                        durTime = 2000\n" +
            "                        interpolator = InterpolatorEnum.Accelerate.type\n" +
            "                    }\n" +
            "                }\n" +
            "            }"

    private var anim_1: View? = null
    private var anim_2: View? = null
    private var anim_3: View? = null
    private var anim_surface: com.base.animation.AnimSurfaceView? = null
    private lateinit var mCode: android.widget.TextView
    private lateinit var mNinePTxt: android.widget.TextView
    private lateinit var mTextXml: android.widget.TextView
    private lateinit var relative: android.view.ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_anim_canvas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        anim_1 = view.findViewById(R.id.anim_1)
        anim_2 = view.findViewById(R.id.anim_2)
        anim_3 = view.findViewById(R.id.anim_3)
        anim_surface = view.findViewById(R.id.anim_surface)
        mCode = view.findViewById(R.id.mCode)
        mNinePTxt = view.findViewById(R.id.mNinePTxt)
        mTextXml = view.findViewById(R.id.mTextXml)
        relative = view.findViewById(R.id.relative)
        lifecycleScope.launch {
            mNinePTxt.background = loadNinePatchDrawable(
                this@TestAnimCanvasFragment.context,
                "http://imgs.pago.tv/products/8e09145b-4521-4184-a405-dd08882df3d3.png", true
            )
        }
        //mGradientColorAnimTextView?.starter()

        mTextXml.text = xml
        mCode.text = code

        anim_1?.setOnClickListener {
            lifecycleScope.launch {
                repeat(100) {
                    startMoreAnim21()
                    delay(50)
                }
            }
        }

        anim_2?.setOnClickListener {
            lifecycleScope.launch {
                startImageDouAnim2()
            }
        }

        anim_3?.setOnClickListener {
            startImageDouAnim3()
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

    private fun startSingleAnimOrigin() {
        val view = ImageView(context)
        relative.addView(view)
        view.visibility = View.GONE
        val size = 80
        val bitmap =
            BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, size, size)
        view.setImageBitmap(bitmap)
        val translationAnimatorSet = AnimatorSet()
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationX", 0f,
            DisplayUtils.getScreenWidth(this.activity).toFloat() / 2 - size
        ).setDuration(1000)
        val animator1 = ObjectAnimator.ofFloat(
            view,
            "translationY", 0f,
            DisplayUtils.getScreenHeight(this.activity).toFloat() / 2 - size
        ).setDuration(1000)
        val animator2 = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).setDuration(1000)
        val animator3 = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f).setDuration(1000)
        val animator4 = ObjectAnimator.ofFloat(
            view,
            "translationY", 0f,
        ).setDuration(1000)
        val animator5 = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f).setDuration(1000)
        val animator6 = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f).setDuration(1000)
        //translationAnimatorSet.playTogether(animator, animator1, animator2, animator3)
        translationAnimatorSet
            .play(animator).with(animator1).with(animator2)
            .with(animator3).before(AnimatorSet().apply {
                play(animator4).with(animator5).with(animator6)
            })
        translationAnimatorSet.interpolator = AccelerateDecelerateInterpolator()
        translationAnimatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                view.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                relative.removeView(view)
            }

            override fun onAnimationCancel(animation: Animator) {
                view.visibility = View.GONE
                relative.removeView(view)
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })
        translationAnimatorSet.start()
    }

    private fun startSingleAnim() {
        val size = 80
        val url = "https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png"
        AnimEncoder().buildAnimNode {
            imageNode {
                this.url = url
                this.displayHeightSize = size
                startNode {
                    layoutIdName = "test1"
                    point = PointF(0f, 0f)
                    scaleX = 0.5f
                    scaleY = 0.5f
                    endNode {
                        layoutIdName = "test2"
                        point = PointF(500f, 500f)
                        scaleX = 3f
                        scaleY = 3f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                    endNode {
                        layoutIdName = "test3"
                        point = PointF(500f, 0f)
                        scaleX = 0.5f
                        scaleY = 0.5f
                        durTime = 2000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                }
            }
        }.apply {
            Log.i("zzc", this.buildString())
            lifecycleScope.launch(Dispatchers.IO) {
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(surface, this@apply) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
                        }
                    }
                    displayItem
                }
            }
        }
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
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                .toFloat() / 2 - size / 2,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                .toFloat() / 2 - size / 2
                        )
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
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2, size / 2f
                            )
                            scaleX = 1f
                            scaleY = 1f
                            endNode {
                                point = PointF(
                                    0f,
                                    DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                        .toFloat() / 2 - size / 2
                                )
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
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2, size / 2f
                            )
                            scaleX = 1f
                            scaleY = 1f
                            durTime = 2000
                            interpolator = InterpolatorEnum.Accelerate.type
                        }
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2,
                                DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                    .toFloat() - size / 2
                            )
                            scaleX = 2f
                            scaleY = 2f
                            durTime = 1000
                            interpolator = InterpolatorEnum.Linear.type
                        }
                    }
                    endNode {
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                .toFloat() - size / 2,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                .toFloat() / 2 - size / 2
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
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2, size / 2f
                            )
                            scaleX = 0f
                            scaleY = 0f
                            durTime = 1000
                            interpolator = InterpolatorEnum.Linear.type
                        }
                    }
                }
            }
        }.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    surface,
                    this@apply,
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap =
                                BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
                        }
                    }
                    displayItem
                }
            }
        }
    }


    private fun startSingleAnim3() {
        lifecycleScope.launch(Dispatchers.IO) {
            val surface = anim_surface ?: return@launch
            AnimDecoder2.suspendPlayAnimWithXml(surface, xml) { node, displayItem ->
                when (displayItem) {
                    is BitmapDisplayItem -> {
                        displayItem.mBitmap =
                            BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
                    }
                }
                displayItem
            }
        }
    }


//    private fun startMoreAnim3() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            AnimDecoder.suspendPlayAnimWithXml(xmlMore, anim_surface, xmlMore) {
//                BitmapFactory.decodeResource(resources, R.mipmap.xin)
//            }
//        }
//    }

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
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context).toFloat() / 2,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context).toFloat() / 2
                        )
                        scaleX = 2f
                        scaleY = 2f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Decelerate.type
                    }
                    endContainer {
                        durTime = 3000
                        endNode {
                            durTime = 1000
                            rotation = 360f
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2, 0f
                            )
                        }
                        endNode {
                            point = PointF(
                                0f,
                                DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2
                            )
                            alpha = 0
                        }
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2,
                                DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                    .toFloat()
                            )
                            scaleX = 0f
                            scaleY = 0f
                        }
                        layoutNode {
                            this.layoutIdName = "view_test_layout"
                            endNode {
                                point = PointF(
                                    DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                        .toFloat(),
                                    DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                        .toFloat() / 2 - size / 2
                                )
                                durTime = 1000
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
            Animer.log.i("tttt2", this.buildString())
            lifecycleScope.launch {
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    surface, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap =
                                BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
                        }
                    }
                    displayItem
                }
            }
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
                        DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context) / 2f,
                        DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context) / 1f
                    )
                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context) / 2f,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context) / 2f
                        )
                        durTime = 1000
                        interpolator = InterpolatorEnum.Decelerate.type
                    }
                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context) / 2f,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context) / 1f
                        )
                        durTime = 2000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                }
            }
        }.buildString().apply {
            lifecycleScope.launch {
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithXml(
                    surface, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is ImageDouNode.BitmapDouDisplay -> {
                            loadImage(fragment = this@TestAnimCanvasFragment, (node as ImageNode).url, node.displayHeightSize)?.let {
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

    private fun startImageDouAnim3() {
        AnimEncoder().buildAnimNode {
            layoutNode {
                layoutIdName = "view_liuguang"
                startNode {
                    scaleX = 1f
                    scaleY = 1f
                    point = PointF(
                        DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                            .toFloat(),
                        DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                            .toFloat() / 3
                    )
                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            0f,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                .toFloat() / 3
                        )
                        durTime = 1500
                        interpolator = InterpolatorEnum.Decelerate.type
                    }

                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            0f,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                .toFloat() / 3
                        )
                        durTime = 500
                        interpolator = InterpolatorEnum.Linear.type
                    }
                    endNode {
                        scaleX = 1f
                        scaleY = 1f
                        point = PointF(
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                .toFloat() * -1,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                .toFloat() / 3
                        )
                        durTime = 1000
                        interpolator = InterpolatorEnum.Linear.type
                    }
                }
            }
        }.apply {
            lifecycleScope.launch {
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    surface, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is LayoutDisplayItem -> {

                        }
                    }
                    displayItem
                }
            }
        }
    }

    /**
     * 动画雨
     */
    private fun startAnimRain() {
        val size = 300
        val url = "https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png"
        val height = DisplayUtils.getScreenHeight(this.activity).toFloat()
        val indexSize = 20
        val checkOverlapping = mutableMapOf<Long, Int>()
        for (i in 0 until indexSize) {
            val locationX = getLocationX(size, 3000L, 300L, checkOverlapping)
            checkOverlapping[locationX.delayTime] = locationX.itemLocationX

            val node = AnimEncoder().buildAnimNode {
                imageNode {
                    this.url = url
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



            lifecycleScope.launch {
                delay(locationX.delayTime)
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(surface, node) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = red
                        }
                    }
                    displayItem
                }
            }
        }
    }

    private fun getLocationX(size: Int, totalTime: Long, duringTime: Long, checkOverlapping: MutableMap<Long, Int>): LocationX {
        val width = DisplayUtils.getScreenWidth(this.activity)
        var time = (0L..totalTime).random()
        var itemLocationX = (size / 2..(width - size / 2)).random()
        var timeList = checkOverlapping.filter { abs(it.key - time) < duringTime }
        Log.i("ttt", "timeList:${timeList.size}")
        while (timeList.size > 2) {
            time = (0L..totalTime).random()
            timeList = checkOverlapping.filter { abs(it.key - time) < duringTime }
            Log.i("ttt", "update timeList:${timeList.size}")
        }
        Log.i("ttt", "itemLocationX:$itemLocationX time:$time")
        var itemList = timeList.filter { abs(itemLocationX - it.value) < size / 2 }
        Log.i("ttt", "itemList:${itemList.size}")
        var i = 0
        while (itemList.isNotEmpty()) {
            itemLocationX = (size / 2..(width - size / 2)).random()
            itemList = timeList.filter { abs(itemLocationX - it.value) < size / 2 }
            i++
            if (i > 5) {
                return getLocationX(size, totalTime, duringTime, checkOverlapping)
            }
            Log.i("ttt", "update itemList:${itemList.size} itemLocationX:$itemLocationX timeList:${timeList.values.map { it.toString() }}")
        }

        return LocationX().apply {
            this.itemLocationX = itemLocationX
            this.delayTime = time
        }
    }

    inner class LocationX {
        var itemLocationX: Int = -1
        var delayTime: Long = 0L
    }

    override fun onDestroyView() {
        super.onDestroyView()
        anim_surface?.removeAnimListener(null)
    }


    override fun itemClick(animId: Long, animDrawObject: AnimDrawObject, touchPointF: PointF, itemCenterPointF: PointF, extra: String) {
        Toast.makeText(this@TestAnimCanvasFragment.context, "$animId", Toast.LENGTH_SHORT).show()
        anim_surface?.removeAnimId(animId)
        val size = 100
        val url = "https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png"
        val width = DisplayUtils.getScreenWidth(this.activity).toFloat()
        val height = DisplayUtils.getScreenHeight(this.activity).toFloat()
        val node = AnimEncoder().buildAnimNode {
            imageBezierNode {
                this.url = url
                this.displayHeightSize = size
                startNode {
                    point = itemCenterPointF
                    scaleX = 1f
                    scaleY = 1f
                    alpha = 255
                    endNode {
                        point = PointF(width / 2f, height)
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 100
                        durTime = 2000L
                        interpolator = InterpolatorEnum.Decelerate.type
                    }
                }
            }
        }
        lifecycleScope.launch {
            val surface = anim_surface ?: return@launch
            AnimDecoder2.suspendPlayAnimWithAnimNode(surface, node) { node, displayItem ->
                when (displayItem) {
                    is BitmapDisplayItem -> {
                        displayItem.mBitmap = suspendCancellableCoroutine {
                            Glide.with(this@TestAnimCanvasFragment).asBitmap().load(url).into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    it.resume(resource)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                }
                            })
                        }
                    }
                }
                displayItem
            }
        }
    }

    private fun startMoreAnim21() {
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
                            DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                .toFloat() / 2 - size / 2,
                            DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                .toFloat() / 2 - size / 2
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
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2, 0f
                            )
                        }
                        endNode {
                            point = PointF(
                                0f,
                                DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2
                            )
                            alpha = 0
                        }
                        endNode {
                            point = PointF(
                                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                    .toFloat() / 2 - size / 2,
                                DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                    .toFloat()
                            )
                            scaleX = 0f
                            scaleY = 0f
                        }
                        layoutNode {
                            this.layoutIdName = "view_test_layout"
                            endNode {
                                point = PointF(
                                    DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context)
                                        .toFloat(),
                                    DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context)
                                        .toFloat() / 2 - size / 2
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
                val surface = anim_surface ?: return@launch
                AnimDecoder2.suspendPlayAnimWithAnimNode(
                    surface, this@apply
                ) { node, displayItem ->
                    when (displayItem) {
                        is BitmapDisplayItem -> {
                            displayItem.mBitmap = BitmapFactory.decodeResource(resources, R.mipmap.xin)
                        }
                    }
                    displayItem
                }
            }
        }
    }
}

suspend fun loadImage(fragment: Fragment, url: String, size: Int): Bitmap? {
    return suspendCancellableCoroutine {
        Glide.with(fragment).asBitmap()
            .load(url).into(object : CustomTarget<Bitmap>(
                size, size
            ) {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap>?
                ) {
                    if (it.isActive)
                        it.resume(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    if (it.isActive)
                        it.resume(null)
                }
            })
    }
}

/**
 * url 转.9.png
 */
suspend fun loadNinePatchDrawable(
    context: Context?,
    url: String?,
    isMirror: Boolean = false
): NinePatchDrawable? {
    if (url.isNullOrEmpty()) {
        return null
    }
    context ?: return null
    return suspendCancellableCoroutine {
        Glide.with(context).asBitmap().load(url)
            .dontAnimate().into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    val newBitmap = if (isMirror) {
                        val matrix = Matrix()
                        matrix.postScale(-1f, 1f) //镜像垂直翻转
                        Bitmap.createBitmap(
                            resource, 0, 0, resource.width, resource.height, matrix, false
                        )
                    } else {
                        resource
                    }
                    if (NinePatch.isNinePatchChunk(newBitmap.ninePatchChunk)) {
                        Log.i("zzc", "isNinePatchChunk ")
                        it.resume(
                            NinePatchDrawable(
                                context.resources,
                                newBitmap,
                                newBitmap.ninePatchChunk,
                                Rect(),
                                null
                            )
                        )
                    } else {
                        Log.i("zzc", "isNinePatchChunk not")
                        val start = resource.width / 2 - 10
                        val hStart = resource.height / 2 - 5
                        val ninePatch =
                            NinePatchChunk.createInstance(start, start + 20, hStart, hStart + 10)
                                ?.toData() ?: byteArrayOf()
                        Log.i("zzc", "isNinePatchChunk  $ninePatch")
                        val patch = NinePatch(newBitmap, ninePatch, null)
                        it.resume(
                            NinePatchDrawable(context.resources, patch)
                        )
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }
}