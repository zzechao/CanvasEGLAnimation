package com.base.animation.log

import android.util.Log

/**
 * @author:zhouzechao
 * @date: 2/24/21
 * description：
 */
open class DefaultLog : ILog {
    override fun logcatVisible(visible: Boolean) {
    }

    override fun v(tag: String, message: () -> Any?) {
    }

    override fun v(tag: String, format: String, vararg args: Any?) {
    }

    override fun v(tag: String, message: String) {
    }

    override fun d(tag: String, message: () -> Any?) {
    }

    override fun d(tag: String, format: String, vararg args: Any?) {
    }

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun i(tag: String, message: () -> Any?) {
    }

    override fun i(tag: String, format: String, vararg args: Any?) {
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun w(tag: String, message: () -> Any?) {
        Log.w(tag, message()?.toString() ?: "")
    }

    override fun w(tag: String, format: String, vararg args: Any?) {
        Log.w(tag, String.format(format, *args))
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun e(tag: String, message: () -> Any?, error: Throwable?) {
        Log.e(tag, message()?.toString() ?: "", error)
    }

    override fun e(tag: String, format: String, error: Throwable?, vararg args: Any?) {
        Log.e(tag, String.format(format, *args), error)
    }

    override fun e(tag: String, message: String, error: Throwable?) {
        Log.e(tag, message, error)
    }
}