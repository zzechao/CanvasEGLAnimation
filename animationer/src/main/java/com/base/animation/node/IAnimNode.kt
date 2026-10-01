package com.base.animation.node

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import androidx.annotation.CallSuper
import com.base.animation.AnimCache.pointLayoutIDCache
import com.base.animation.AnimationEx
import com.base.animation.Animer
import com.base.animation.IAnimView
import com.base.animation.model.PathObject
import com.base.animation.tryCatch
import com.base.animation.xml.AnimDecoder2
import com.base.animation.xml.XmlWriterHelper
import com.base.animation.xml.node.AnimAttributeName
import com.base.animation.xml.node.AnimNodeName
import com.base.animation.xml.node.XmlBaseAnimNode
import com.base.animation.xml.node.coder.IXmlObjNodeParser
import java.lang.reflect.Field

private const val TAG = "IAnimNode"

/**
 * @author:zhouzechao
 * description：*
 */
interface IAnimNode : XmlBaseAnimNode, IXmlObjNodeParser {

    val nodeName: String
        get() {
            return javaClass.getAnnotation(AnimNodeName::class.java)?.name ?: ""
        }

    private fun parseAndGetField(write: XmlWriterHelper, field: Field) {
        try {
            val annotation = field.getAnnotation(AnimAttributeName::class.java) ?: return
            val fieldValue = field.get(this) ?: return
            val animNodeName = javaClass.getAnnotation(AnimNodeName::class.java)?.name ?: return
            val key = "${animNodeName}_${annotation.name}"
            AnimDecoder2.mapNodeAttributeCoderMap[key]?.let {
                val value = it.attributeEncode(fieldValue) ?: return
                write.atttibute(annotation.name, value)
            } ?: kotlin.run {
                val attributeCoder = annotation.coder.java.newInstance()
                AnimDecoder2.mapNodeAttributeCoderMap[key] = attributeCoder
                val value = attributeCoder.attributeEncode(fieldValue) ?: return
                write.atttibute(
                    attriName = annotation.name, attriValue = value
                )
            }
        } catch (e: Exception) {
            Animer.log.e(TAG, "[parseAndGetField]:${e}")
        }
    }

    fun encodeSelf(write: XmlWriterHelper) {
    }

    override fun encode(write: XmlWriterHelper) {
        if (nodeName.isNotEmpty()) {
            write.node(nodeName) {
                try {
                    val javaClass = this@IAnimNode.javaClass
                    val fields = this@IAnimNode.javaClass.fields
                    Animer.log.i(
                        TAG, "[encode]:${this@IAnimNode.javaClass} ${fields.size} $javaClass"
                    )
                    for (i in fields.indices) {
                        parseAndGetField(this, fields[i])
                    }
                } catch (e: Exception) {
                    Animer.log.e(TAG, "[encode]:${e}")
                }
                encodeSelf(this)
            }
        }
    }

    override fun decode(id: String, anim: IAnimView): PathObject? {
        return null
    }

    @CallSuper
    override fun setAttribute(
        name: String, value: String
    ) {
        try {
            val fields = this.javaClass.fields
            for (i in fields.indices) {
                if (parseAndSetField(fields[i], name, value)) return
            }
        } catch (_: Exception) {
        }
    }

    private fun parseAndSetField(field: Field, name: String, value: String): Boolean {
        try {
            val animNodeName =
                javaClass.getAnnotation(AnimNodeName::class.java)?.name ?: return false
            val annotation = field.getAnnotation(AnimAttributeName::class.java) ?: return false
            val key = "${animNodeName}_${annotation.name}"
            if (annotation.name.equals(name, true)) {
                AnimDecoder2.mapNodeAttributeCoderMap[key]?.let {
                    Animer.log.i(TAG, "[parseAndSetField]:${field.type.kotlin} $value")
                    it.attributeDecode(field.type.kotlin, value)?.let {
                        field.set(this, it)
                    }
                } ?: kotlin.run {
                    val attributeCoder = annotation.coder.java.newInstance()
                    AnimDecoder2.mapNodeAttributeCoderMap[key] = attributeCoder
                    Animer.log.i(TAG, "[parseAndSetField]:${field.type.kotlin} $value")
                    attributeCoder.attributeDecode(field.type.kotlin, value)?.let {
                        field.set(this, it)
                    }
                }
                return true
            }
        } catch (e: Exception) {
        }
        return false
    }

    fun getLayoutIdPoint(idLayout: Int, init: () -> Point): Point {
        if (idLayout == View.NO_ID) {
            return init.invoke()
        }
        return pointLayoutIDCache[idLayout] ?: init.invoke().apply {
            pointLayoutIDCache[idLayout] = this
        }
    }

    fun getCenterOfViewLocationInScreen(view: View): IntArray {
        val pos = IntArray(2)
        val point = getLayoutIdPoint(view.id) {
            val globalRect = Rect()
            view.getGlobalVisibleRect(globalRect)
            Point(globalRect.centerX(), globalRect.centerY())
        }
        pos[0] = point.x
        pos[1] = point.y
        return pos
    }

    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(): Int {
        var height = 0
        val resourceId: Int = tryCatch {
            AnimationEx.mApplication?.resources?.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
        } ?: 0
        if (resourceId > 0) {
            height = AnimationEx.mApplication?.resources?.getDimensionPixelSize(resourceId) ?: 0
        }
        return height
    }
}