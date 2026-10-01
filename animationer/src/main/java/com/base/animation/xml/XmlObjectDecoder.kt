package com.base.animation.xml


import android.util.Xml
import com.base.animation.Animer
import com.base.animation.node.IAnimNode
import com.base.animation.xml.node.AnimNodeName
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.StringReader

private const val TAG = "XmlObjectBuilder"

typealias OnCreateNode = ((name: String) -> IAnimNode)

open class XmlObjectDecoder {

    protected var mapNodeCreatetor = mutableMapOf<String, OnCreateNode>()


    open fun createObject(animXml: String): IAnimNode? {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(animXml.trim()))
            onSkipHead(parser)
            return createObject(null, parser)
        } catch (e: Exception) {
            Animer.log.e(TAG, "->createObject:$e")
        }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun createObject(parent: IAnimNode?, parser: XmlPullParser): IAnimNode? {
        return createObject(parent, parser, 1)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun createObject(
        parent: IAnimNode?,
        parser: XmlPullParser,
        recursiveCallLevel: Int
    ): IAnimNode? {
        var obj: IAnimNode? = null
        var eventType = parser.eventType
        val depth = parser.depth
        Animer.log.i(TAG, "[createObject$recursiveCallLevel] current depth=$depth eventType=$eventType")
        if (eventType != XmlPullParser.START_TAG) return parent
        while (eventType != XmlPullParser.END_TAG) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    Animer.log.i(
                        TAG,
                        "[createObject$recursiveCallLevel] START_TAG ${parser.name} ${parser.depth}"
                    )
                    val curDepth = parser.depth
                    if (curDepth > depth) {
                        createObject(obj, parser, recursiveCallLevel + 1)
                    } else if (curDepth == depth) {
                        val name = parser.name ?: ""
                        if (parent != null) {
                            obj = parent.createChildNode(name)
                        }
                        if (obj == null) {
                            obj = createChildNode(name)
                        }
                        if (obj != null) {
                            //设置属性
                            var k = 0
                            while (k < parser.attributeCount) {
                                Animer.log.i(
                                    TAG, "attribute ${parser.getAttributeName(k)}- ${
                                        parser.getAttributeValue(k)
                                    }"
                                )
                                onSetAttribute(
                                    obj,
                                    parser.getAttributeName(k),
                                    parser.getAttributeValue(k)
                                )
                                ++k
                            }
                            //添加到父节点
                            parent?.addNode(obj)
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    Animer.log.i(
                        TAG, "[createObject$recursiveCallLevel]" +
                                " TEXT ${parser.text} $depth with=${parser.isWhitespace}"
                    )
                    if (!parser.isWhitespace && !parser.text.isNullOrEmpty()) {
                        obj?.setText(parser.text)
                    }
                }
            }
            eventType = parser.next()
        }
        return parent ?: obj
    }

    protected fun createChildNode(name: String): IAnimNode? {
        return mapNodeCreatetor[name]?.invoke(name)
    }

    open fun onSkipHead(parser: XmlPullParser) {
        parser.skipDocumentStart()
    }

    internal fun XmlPullParser.skipDocumentStart() {
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            next()
        }
    }

    protected open fun onSetAttribute(objnode: IAnimNode, name: String, value: String) {
        Animer.log.i(TAG, "onSetAttribute name:$name value:$value")
        objnode.setAttribute(name, value)
    }

    fun <T : IAnimNode> registerNodeCreatetor(clazz: Class<T>) {
        val name = clazz.getAnnotation(AnimNodeName::class.java)?.name
        if (name?.isNotEmpty() == true) {
            mapNodeCreatetor[name] = { clazz.newInstance() }
        }
    }
}