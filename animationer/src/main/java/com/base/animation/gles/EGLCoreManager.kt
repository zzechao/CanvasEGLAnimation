package com.base.animation.gles

import android.opengl.*
import com.base.animation.Animer
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author zzechao
 * @date 2026/10/1
 * @desc EGL全局核心管理器，负责管理全局共享EGLContext与共享纹理池
 * 支持多个 EGLAnimView（如双层重叠View）跨上下文公用同一个纹理ID，杜绝重复显存分配与重复纹理上传
 */
object EGLCoreManager {
    private const val TAG = "EGLCoreManager"

    /**
     * 全局共享纹理池
     */
    val sharedTexturePools: EGLTexturePools by lazy { EGLTexturePools() }

    private val refCount = AtomicInteger(0)

    @Volatile
    private var rootDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    @Volatile
    private var rootConfig: EGLConfig? = null
    @Volatile
    private var rootContext: EGLContext = EGL14.EGL_NO_CONTEXT
    @Volatile
    private var rootSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    /**
     * 获取或初始化全局共享Root EGLContext
     */
    @Synchronized
    fun obtainSharedContext(): EGLContext {
        refCount.incrementAndGet()
        if (rootContext != EGL14.EGL_NO_CONTEXT) {
            return rootContext
        }
        try {
            rootDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (rootDisplay == EGL14.EGL_NO_DISPLAY) {
                Animer.log.w(TAG, "obtainSharedContext: unable to get EGL14 display")
                return EGL14.EGL_NO_CONTEXT
            }

            val version = IntArray(2)
            if (!EGL14.eglInitialize(rootDisplay, version, 0, version, 1)) {
                Animer.log.w(TAG, "obtainSharedContext: unable to initialize EGL14")
                return EGL14.EGL_NO_CONTEXT
            }

            var configs = arrayOfNulls<EGLConfig>(1)
            var numConfigs = IntArray(1)
            var attribList = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            if (!EGL14.eglChooseConfig(rootDisplay, attribList, 0, configs, 0, 1, numConfigs, 0) || configs[0] == null) {
                attribList = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
                )
                if (!EGL14.eglChooseConfig(rootDisplay, attribList, 0, configs, 0, 1, numConfigs, 0) || configs[0] == null) {
                    Animer.log.w(TAG, "obtainSharedContext: unable to choose config")
                    return EGL14.EGL_NO_CONTEXT
                }
            }
            rootConfig = configs[0]

            val attrib2List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            rootContext = EGL14.eglCreateContext(rootDisplay, rootConfig, EGL14.EGL_NO_CONTEXT, attrib2List, 0)
            if (rootContext == EGL14.EGL_NO_CONTEXT) {
                Animer.log.w(TAG, "obtainSharedContext: eglCreateContext root failed")
                return EGL14.EGL_NO_CONTEXT
            }

            // 创建 1x1 离屏 pbuffer surface 并做一次初始化绑定后解绑，保证所有驱动上 root 上下文状态完全就绪
            val pbufferAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )
            rootSurface = EGL14.eglCreatePbufferSurface(rootDisplay, rootConfig, pbufferAttribs, 0)
            if (rootSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(rootDisplay, rootSurface, rootSurface, rootContext)
                EGL14.eglMakeCurrent(rootDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            }
            Animer.log.i(TAG, "obtainSharedContext: root context created successfully: $rootContext")
        } catch (e: Throwable) {
            Animer.log.e(TAG, "obtainSharedContext exception: $e")
            rootContext = EGL14.EGL_NO_CONTEXT
        }
        return rootContext
    }

    /**
     * 释放共享 EGLContext 引用计数，当所有 View 都销毁时清理资源
     */
    @Synchronized
    fun releaseSharedContext() {
        val count = refCount.decrementAndGet()
        if (count <= 0) {
            refCount.set(0)
            if (rootContext != EGL14.EGL_NO_CONTEXT) {
                Animer.log.i(TAG, "releaseSharedContext: destroying root shared EGLContext")
                if (rootSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglMakeCurrent(rootDisplay, rootSurface, rootSurface, rootContext)
                    sharedTexturePools.clear()
                    EGL14.eglMakeCurrent(rootDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroySurface(rootDisplay, rootSurface)
                    rootSurface = EGL14.EGL_NO_SURFACE
                } else {
                    sharedTexturePools.clear()
                }
                EGL14.eglDestroyContext(rootDisplay, rootContext)
                rootContext = EGL14.EGL_NO_CONTEXT
                rootDisplay = EGL14.EGL_NO_DISPLAY
                rootConfig = null
            }
        }
    }
}
