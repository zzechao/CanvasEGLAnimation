package com.base.animation.gles

import android.opengl.GLES20
import com.base.animation.Animer
import com.base.animation.gles.utils.GLESUtils

/**
 * @author zzechao
 * @date 2025/3/20 18:39
 * 着色器
 */
class EGLAnimShader {
    companion object {
        private const val TAG = "EGLAnimShader"
    }

    // 顶点着色器代码
    private val vertexShaderCode = """uniform mat4 uMVPMatrix;
        uniform mat4 uTexMatrix;
        attribute vec4 vPosition;
        attribute vec2 vTexCoordinate;
        varying vec2 aTexCoordinate;
        void main() {
          gl_Position = uMVPMatrix * vPosition;
          aTexCoordinate = (uTexMatrix * vec4(vTexCoordinate, 0.0, 1.0)).xy;
        }
        """

    // 片段着色器代码
    private val fragmentShaderCode = """#extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform sampler2D vTexture;
            uniform samplerExternalOES vTextureOES;
            uniform float uAlpha;
            uniform bool uIsColor2D;
            varying vec2 aTexCoordinate;
            
            void main() {
                if (uIsColor2D) {
                    gl_FragColor = texture2D(vTexture, aTexCoordinate) * uAlpha;
                } else {
                    gl_FragColor = texture2D(vTextureOES, aTexCoordinate) * uAlpha;
                }
            }
        """

    private var mProgram = 0
    private var vertexShaderId = 0
    private var fragmentShaderId = 0

    var positionHandle = 0

    // 纹理坐标句柄
    var texCoordinateHandle = 0

    // 纹理Texture句柄
    var texHandle = 0

    // Use to access and set the view transformation
    var vPMatrixHandle = 0

    var uTexMatrixHandle = 0

    var uAlphaHandle = 0

    var vTextureOESHandle = 0

    var uIsColor2DHandle = 0

    fun initShader() {
        vertexShaderId = GLESUtils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        fragmentShaderId = GLESUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            Animer.log.e(TAG, "Shader compilation failed! vertexId: $vertexShaderId, fragmentId: $fragmentShaderId")
            return
        }

        mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vertexShaderId)
        GLES20.glAttachShader(mProgram, fragmentShaderId)
        GLES20.glLinkProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        texCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate")
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        uTexMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uTexMatrix")
        texHandle = GLES20.glGetUniformLocation(mProgram, "vTexture")
        vTextureOESHandle = GLES20.glGetUniformLocation(mProgram, "vTextureOES")
        uAlphaHandle = GLES20.glGetUniformLocation(mProgram, "uAlpha")
        uIsColor2DHandle = GLES20.glGetUniformLocation(mProgram, "uIsColor2D")

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(mProgram)
            Animer.log.e(TAG, "glLinkProgram failed: $log")
            GLES20.glDeleteProgram(mProgram)
            mProgram = 0
        }
    }

    fun useShader() {
        if (mProgram != 0) {
            GLES20.glUseProgram(mProgram)
        }
    }

    fun unUseShader() {
        GLES20.glUseProgram(0)
    }

    fun destroyShader() {
        Animer.log.i(TAG, "destroyShader")
        unUseShader()

        if (positionHandle >= 0) {
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
        if (texCoordinateHandle >= 0) {
            GLES20.glDisableVertexAttribArray(texCoordinateHandle)
        }

        if (mProgram != 0) {
            if (vertexShaderId != 0) {
                GLES20.glDetachShader(mProgram, vertexShaderId)
                GLES20.glDeleteShader(vertexShaderId)
                vertexShaderId = 0
            }
            if (fragmentShaderId != 0) {
                GLES20.glDetachShader(mProgram, fragmentShaderId)
                GLES20.glDeleteShader(fragmentShaderId)
                fragmentShaderId = 0
            }
            GLES20.glDeleteProgram(mProgram)
            mProgram = 0
        }
        GLES20.glReleaseShaderCompiler()
        Animer.log.i(TAG, "destroyShader end")
    }
}