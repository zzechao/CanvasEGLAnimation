package com.base.animation.gles

import android.opengl.*
import com.base.animation.Animer

/**
 * EGL环境搭建类
 * 1.创建显示屏幕类EGLDisplay
 * 2.配置FrameBuffer类EGLConfig
 * 3.创建FrameBuffer的EGLSurface
 * 4.创建上下文EGLContext，并与Surface绑定
 */
class EGLHelper {

    companion object {
        private val TAG: String = EGLHelper::class.java.simpleName
    }

    /**
     * 屏幕显示类，表示一个可以现实的屏幕
     */
    private var mEGLDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY

    /**
     * 系统窗口或FrameBuffer
     */
    private var mEGLSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    /**
     * FrameBuffer的配置属性
     */
    private var mEGLConfig: EGLConfig? = null

    /**
     * 渲染上下文，用于绑定上面3个属性，将其关联起来
     */
    private var mEGLContext: EGLContext = EGL14.EGL_NO_CONTEXT

    val eglContext: EGLContext get() = mEGLContext
    val eglDisplay: EGLDisplay get() = mEGLDisplay

    /**
     * 初始化EGL环境
     *
     * @param surface
     * @param shareContext 用于跨View共享纹理与显存缓冲的EGL上下文
     */
    fun initEGL(surface: Any, shareContext: EGLContext = EGL14.EGL_NO_CONTEXT) {
        // 1、获取显示设备
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("unable to get EGL14 display")

        // 2、初始化EGL
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = EGL14.EGL_NO_DISPLAY
            throw RuntimeException("unable to initialize EGL14")
        }

        // 3、资源配置，例如颜色配置等
        val attribList = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  // 指定渲染api类别
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        // 4、ChooseConfig
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
            throw RuntimeException("unable to find RGB8888 / ${version[0]}.${version[1]} EGLConfig")
        }
        mEGLConfig = configs[0]

        // 5、创建上下文（支持传入 shareContext 共享纹理命名空间）
        val attrib2List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, shareContext, attrib2List, 0)
        if (context == EGL14.EGL_NO_CONTEXT) throw RuntimeException("eglCreateContext error")
        mEGLContext = context

        // 6、创建渲染Surface
        val attribList1 = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface, attribList1, 0)
        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("eglCreateWindowSurface error: surface was null or invalid")
        }
        mEGLSurface = eglSurface

        // 7、将EGL上下文绑定到当前线程，实现渲染环境的设置，之后就可以使用OpenGL进行绘制了
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
        Animer.log.i(TAG, "egl init success! context: $mEGLContext")
    }

    /**
     * 交换缓冲区
     */
    fun swapBuffers() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY && mEGLSurface != EGL14.EGL_NO_SURFACE) {
            if (!EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)) {
                val error = EGL14.eglGetError()
                Animer.log.w(TAG, "eglSwapBuffers failed with error: 0x${Integer.toHexString(error)}")
            }
        }
    }

    /**
     * 销毁EGL环境
     */
    fun destroyEGL() {
        Animer.log.d(TAG, "egl destroy!")
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)

        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY && mEGLSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            mEGLSurface = EGL14.EGL_NO_SURFACE
        }

        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY && mEGLContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            mEGLContext = EGL14.EGL_NO_CONTEXT
        }

        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
            mEGLDisplay = EGL14.EGL_NO_DISPLAY
        }

        mEGLConfig = null
        Animer.log.d        (TAG, "egl destroy success!")
    }
}