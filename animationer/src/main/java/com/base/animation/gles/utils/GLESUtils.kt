package com.base.animation.gles.utils

import android.opengl.GLES20
import com.base.animation.Animer

/**
 * @author zzechao
 * @date 2025/3/20 10:41
 */
object GLESUtils {
    private const val TAG = "GLESUtils"

    /**
     * 加载着色器代码
     *
     * @param type
     * @param shaderCode
     * @return
     */
    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Animer.log.e(TAG, "glCreateShader failed for type: $type")
            return 0
        }
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            Animer.log.e(TAG, "glCompileShader failed for type $type: $log\nShader code:\n$shaderCode")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
