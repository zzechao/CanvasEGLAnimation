package com.base.animation.gles

/**
 * @author zzechao
 * @date 2025/3/21 11:15
 * EGL消息
 */
data class EGLAction(val msg: Int, val action: () -> Unit) {
    companion object {
        const val MSG_INIT = 0
        const val MSG_PLAY = 1
        const val MSG_PAUSE = 2
        const val MSG_RESUME = 3
        const val MSG_RELEASE = 4
        const val MSG_SIZE_CHANGED = 5
        const val MSG_DESTROY = 6
    }

    fun description(): String {
        return when (msg) {
            MSG_INIT -> "MSG_INIT"
            MSG_PLAY -> "MSG_PLAY"
            MSG_PAUSE -> "MSG_PAUSE"
            MSG_RESUME -> "MSG_RESUME"
            MSG_RELEASE -> "MSG_RELEASE"
            MSG_SIZE_CHANGED -> "MSG_SIZE_CHANGED"
            MSG_DESTROY -> "MSG_DESTROY"
            else -> ""
        }
    }
}
