package com.base.animation.gles.utils

import android.os.Trace

/**
 * Systrace / Perfetto 跟踪辅助工具
 * 用于高精度度量 EGL / Canvas 渲染管线各阶段耗时
 */
object TraceEnabler {
    init {
        try {
            val method = Trace::class.java.getMethod("setAppTracingAllowed", Boolean::class.javaPrimitiveType)
            method.invoke(null, true)
        } catch (_: Throwable) {
        }
    }
}

inline fun <T> traceSection(name: String, block: () -> T): T {
    TraceEnabler
    Trace.beginSection(name)
    try {
        return block()
    } finally {
        Trace.endSection()
    }
}
