# CanvasEGLAnimation

[![GitHub license](https://img.shields.io/github/license/JailedBird/ArouterGradlePlugin.svg)](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/LICENSE)

## 介绍

`CanvasEGLAnimation` 采用声明式的方式构建动画节点，支持自定义节点的绘制元素以及路径过程节点，支持多种插值器，支持xml远程配置模式，配置方便。
`CanvasEGLAnimation` 使用原生 Android Canvas 库渲染动画，为你提供高性能、低开销的动画体验。
`CanvasEGLAnimation` 1.1.0版本支持EGLTextureView独立线程队列渲染，采用GL渲染，以补全surfaceView针对动画使用不足。

## 效果图

### 做类似的动画效果（间隔50ms播放100个动画）
![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/OScreen_recording.gif)

### 用普通属性动画，内存消耗情况
![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/origin.jpg)

### 使用canvas进行动画绘制,总体只消耗5M内存，比普通的属性动画优化43M内存
![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/optimized.jpg)


## 用法
根目录build.gradle中添加mavenCentral()

```groovy
    repositories {
        mavenCentral()
    }
```

model build.gradle添加

```groovy
     implementation "io.github.zzechao:canvasanimation:1.1.3"
```

当前版本：1.1.3

### 动画库初始化

```kotlin
    /**
     * @param application 应用的Application
     * @param displayMaxCacheSize 缓存display的大小（这里有DisplayItem的重用逻辑，根据内部声称key）
     * @param mode 模式1：计算动画节点预先处理，模式2：计算动画节点，根据每帧时长实时计算
     * @param nodeClazzs 解码器节点的注册(节点class),xml要使用自定义displayItem的节点就要先注册，这样才能解析出node树
     */
    AnimationEx.init(this.application, 200, 2, ImageBezierNode::class.java, ImageDouNode::class.java)

    /**
     * @param nodeClazz - 节点class 解码器节点的注册(节点class),xml要使用自定义displayItem的节点就要先注册，这样才能解析出node树
     */
    AnimationEx.registerNode(ImageDouNode::class.java)
```

### 构建动画的节点（代码构建）
```kotlin
    /**
     * 代码创建一个绘制节点整个过程，AnimEncoder编码器；
     * imageNode是图片的节点（除了imageNode还有TextNode、layoutNode、自定义的Node的DisplayItem，可以理解为绘制的元素）；
     * startNode是开始节点（坐标有两种定义方式，一个是根据layout的id对应res中ids的name获取）；
     * endNode结束节点（和startNode类似）；
     * 还有一种情况是一开始节点，多个结束节点，类似送礼同时分散到多个麦位的，可以用endNodeContainer包含多个endNode
     */
    val animNode = AnimEncoder().buildAnimNode {
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
                        point = PointF(100f, 100f)
                        scaleX = 3f
                        scaleY = 3f
                        durTime = 1000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                    endNode {
                        layoutIdName = "test3"
                        point = PointF(100f, 0f)
                        scaleX = 0.5f
                        scaleY = 0.5f
                        durTime = 2000
                        interpolator = InterpolatorEnum.Accelerate.type
                    }
                }
            }
        }
```

## 执行方式 
```kotlin 
    /**
     * 代码构建节点通过这个方式直接运行；
     * 调用有个闭包的回调方式，用来做上层修改绘制元素的变量设置以及特定拦截操作，Glide加载，
     * 以及通过一些请求返回的数据修改layout里的字段等等，采用挂起协程的方式进行
     */
    AnimDecoder2.suspendPlayAnimWithAnimNode(
        anim_surface,
        animNode,
    ) { node, displayItem ->
        when (displayItem) {
            is BitmapDisplayItem -> {
                displayItem.mBitmap =
                    BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
            }
        }
        displayItem
    }
```

### xml的方式构建节点（远端返回对应字符串进行解析） 
```xml 
    <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
    <anim>
        <imageNode displaySize="80" url="https://turnover-cn.oss-cn-hangzhou.aliyuncs.com/turnover/1670379863915_948.png">
            <startAnim alpha="255" startIdName="test1" startL='{"x":0.0,"y":0.0}' rotation="0.0" scaleX="0.5" scaleY="0.5">
                <endAnim alpha="255" durTime="1000" interpolator="1" endIdName="test2" endL='{"x":100.0,"y":100.0}' rotation="0.0" scaleX="3.0" scaleY="3.0" url="" />
                <endAnim alpha="255" durTime="2000" interpolator="1" endIdName="test3" endL='{"x":100.0,"y":0.0}' rotation="0.0" scaleX="0.5" scaleY="0.5" url="" />
            </startAnim>
        </imageNode>
    </anim>
```

## 执行方式
```kotlin 
    /**
     * 与代码构建节点调用方式类似
     */
    AnimDecoder2.suspendPlayAnimWithXml(anim_surface, xml) { node, displayItem ->
        when (displayItem) {
            is BitmapDisplayItem -> {
                displayItem.mBitmap =
                    BitmapLoader.decodeBitmapFrom(resources, R.mipmap.xin, 1, 100, 100)
            }
        }
        displayItem
    }
```

### 动画的效果演示，压缩成gif可能导致画质损伤，可参考项目里的mp4更为清晰顺畅
![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/v1.gif)

![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/v2.gif)

### 提供自定义的绘制元素节点以及路径过程节点，效果图
![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/v3.gif)

### 仿支付宝的红包雨效果
![image](https://github.com/zzechao/CanvasAnimation/blob/canvas_view_new_feature_2.0/v4.gif)

### 自定义节点以及ItemDisplay绘制
```kotlin 
/**
 * 自定义声明式节点
 */
class ImageDouNode : ImageNode(), IXmlDrawableNodeDealIntercept {

    @AnimAttributeName("rocation", DefaultAttributeCoder::class)
    @JvmField
    var rocation = 5

    override var displayItem: KClass<out BaseDisplayItem> = BitmapDouDisplay::class

    override val dealIntercept: IDealNodeDealIntercept = object : IDealNodeDealIntercept {
        override suspend fun invoke(
            displayObject: DisplayObject,
            animNode: IAnimNode,
            chain: AnimNodeChain,
            dealDisplayItem: DealDisplayItem
        ): String {
            if (animNode is ImageDouNode) {
                val key =
                    animNode.url + animNode.displayHeightSize + animNode.nodeName
                val bitmapKey = animNode.url + animNode.displayHeightSize + animNode.nodeName
                val displayId = displayObject.suspendAdd(
                    key = key, kClass = animNode.displayItem
                ) {
                    val bitmapDisplayItem = BitmapDouDisplay(rocation)
                    dealDisplayItem.invoke(animNode, bitmapDisplayItem) // 代理出去处理图片的加载方式
                    val bitmapWidth = bitmapDisplayItem.mBitmap?.width ?: return@suspendAdd null
                    val bitmapHeight = bitmapDisplayItem.mBitmap?.height ?: return@suspendAdd null
                    val displayWidth = animNode.displayHeightSize * bitmapWidth / bitmapHeight
                    bitmapDisplayItem.setDisplaySize(displayWidth, animNode.displayHeightSize)
                    bitmapDisplayItem
                }
                return displayId
            }
            return ""
        }
    }

    /**
     * 自定义绘制的节点计算以及绘制过程
     */
    inner class BitmapDouDisplay(val rocation: Int) : BitmapDisplayItem() {

        override var isCalculate: Boolean = true

        override fun calculate(pathProcess: PathProcess, current: AnimDrawObject, interpolator: BaseInterpolator) {
            super.calculate(pathProcess, current, interpolator)
            val p = pathProcess.curTotalTime / pathProcess.durTime
            val interP = pathProcess.interpolator.getInterpolation(p)
            val inPoint = PointF(
                pathProcess.start.point.x + pathProcess.item.totalX * interP,
                pathProcess.start.point.y + pathProcess.item.totalY * interP
            )
            val alpha = pathProcess.start.alpha + (pathProcess.item.totalAlpha * interP).toInt()
            val scaleX = pathProcess.start.scaleX + pathProcess.item.totalScaleX * interP
            val scaleY = pathProcess.start.scaleY + pathProcess.item.totalScaleY * interP
            val rotation = sin(pathProcess.curTotalTime / 30L) * rocation
            current.reset(inPoint, alpha, scaleX, scaleY, rotation)
        }
    }
}

/**
 * AnimEncoder拓展ImageDouNode节点，构造声明式方法
 */
fun AnimEncoder.imageDouNode(onInit: ImageDouNode.(encoder: AnimEncoder) -> Unit) {
    curNode.addNode(ImageDouNode().apply {
        val lastNode = curNode
        try {
            curNode = this
            onInit(this, this@imageDouNode)
        } finally {
            curNode = lastNode
        }
    })
}

/**
 * 构造自定义绘制itemDisplay的节点动画
 */
AnimEncoder().buildAnimNode {
    imageDouNode { // 自定义绘制节点
        this.rocation = 5
        this.url = url
        this.displayHeightSize = size
        startNode {
            scaleX = 2f
            scaleY = 2f
            point = PointF(
                DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context).toFloat() / 2 - size / scaleX / 2,
                DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context).toFloat() - size / scaleY / 2)
            endNode {
                scaleX = 2f
                scaleY = 2f
                point = PointF(DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context).toFloat() / 2 - size / scaleX / 2,
                    DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context).toFloat() / 2 - size / scaleY / 2)
                durTime = 1000
                interpolator = InterpolatorEnum.Decelerate.type
            }
            endNode {
                scaleX = 2f
                scaleY = 2f
                point = PointF(DisplayUtils.getScreenWidth(this@TestAnimCanvasFragment.context).toFloat() / 2 - size / scaleX / 2,
                    DisplayUtils.getScreenHeight(this@TestAnimCanvasFragment.context).toFloat() - size / scaleY / 2)
                durTime = 2000
                interpolator = InterpolatorEnum.Accelerate.type
            }
        }
    }
}
```

可以参考BitmapDouDisplay、ImageDouNode