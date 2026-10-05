package com.base.animation.gles

import android.graphics.SurfaceTexture
import android.opengl.*
import com.base.animation.Animer
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author zzechao
 * @date 2026/10/2
 * @desc EGL渲染工作组容器
 * 同一个Group内的所有EGLView共享：
 * 1. 同一个后台GL工作Looper线程（具备独立Looper，Choreographer直接入驻，0次跨线程中转）
 * 2. 同一个真实EGLContext（在同一线程内，纹理ID完全天然共享，0跨线程同步损耗，0冲突）
 * 3. 同一个组内纹理池 EGLTexturePools
 */
class EGLRenderGroup(val id: Int) {

    companion object {
        private const val TAG = "EGLRenderGroup"
    }

    val viewCount = AtomicInteger(0)
    val isReleased = AtomicBoolean(false)

    // 专属单线程GL工作Looper线程（系统显示最高优先级）
    private val handlerThread = object : HandlerThread("EGL-GroupThread-$id", android.os.Process.THREAD_PRIORITY_DISPLAY) {
        override fun onLooperPrepared() {
            super.onLooperPrepared()
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY)
            } catch (t: Throwable) {
                // ignore
            }
        }
    }.apply {
        start()
    }
    val handler = Handler(handlerThread.looper)

    // 组内共享纹理池
    val texturePools = EGLTexturePools()

    // 组核心EGL环境
    var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private set
    var eglConfig: EGLConfig? = null
        private set
    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private set

    @Volatile
    private var isEglInitialized = false

    /**
     * 确保组核心EGLContext已初始化（必须在group线程执行）
     */
    fun ensureEGLInitialized() {
        if (isEglInitialized && eglContext != EGL14.EGL_NO_CONTEXT) return

        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                Animer.log.e(TAG, "[$id] ensureEGLInitialized: unable to get EGL14 display")
                return
            }

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                Animer.log.e(TAG, "[$id] ensureEGLInitialized: unable to initialize EGL14")
                return
            }

            var attribList = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0) || configs[0] == null) {
                attribList = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE
                )
                if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0) || configs[0] == null) {
                    Animer.log.e(TAG, "[$id] ensureEGLInitialized: unable to choose config")
                    return
                }
            }
            eglConfig = configs[0]

            val shareContext = EGLCoreManager.obtainSharedContext()
            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, shareContext, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                Animer.log.e(TAG, "[$id] ensureEGLInitialized: eglCreateContext failed")
                return
            }

            isEglInitialized = true
            Animer.log.i(TAG, "[$id] ensureEGLInitialized success: context=$eglContext (shared with root)")
        } catch (t: Throwable) {
            Animer.log.e(TAG, "[$id] ensureEGLInitialized error: $t")
        }
    }

    /**
     * 为具体View创建Window Surface
     */
    fun createSurface(surfaceTexture: SurfaceTexture): EGLSurface {
        if (!isEglInitialized || eglDisplay == EGL14.EGL_NO_DISPLAY || eglConfig == null) {
            ensureEGLInitialized()
        }
        val attribList = intArrayOf(EGL14.EGL_NONE)
        val surface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, attribList, 0)
        if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
            Animer.log.e(TAG, "[$id] createSurface failed: error=0x${Integer.toHexString(EGL14.eglGetError())}")
            return EGL14.EGL_NO_SURFACE
        }
        return surface
    }

    @Volatile
    private var currentSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    /**
     * 将当前工作环境切换到指定View的Surface（同一线程内极轻量切换）
     */
    fun makeCurrent(surface: EGLSurface): Boolean {
        if (surface == EGL14.EGL_NO_SURFACE || eglDisplay == EGL14.EGL_NO_DISPLAY || eglContext == EGL14.EGL_NO_CONTEXT) {
            return false
        }
        if (currentSurface == surface) {
            return true
        }
        val result = EGL14.eglMakeCurrent(eglDisplay, surface, surface, eglContext)
        if (result) {
            currentSurface = surface
            EGL14.eglSwapInterval(eglDisplay, 0)
        }
        return result
    }

    /**
     * 交换缓冲，输出画面
     */
    fun swapBuffers(surface: EGLSurface): Boolean {
        if (surface == EGL14.EGL_NO_SURFACE || eglDisplay == EGL14.EGL_NO_DISPLAY) {
            return false
        }
        return EGL14.eglSwapBuffers(eglDisplay, surface)
    }

    /**
     * 销毁指定View的Surface
     */
    fun destroySurface(surface: EGLSurface) {
        if (surface != EGL14.EGL_NO_SURFACE && eglDisplay != EGL14.EGL_NO_DISPLAY) {
            if (currentSurface == surface) {
                currentSurface = EGL14.EGL_NO_SURFACE
            }
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, surface)
        }
    }

    /**
     * 释放整个工作组（当所有View均注销时调用）
     */
    fun releaseGroup() {
        if (!isReleased.compareAndSet(false, true)) return
        Animer.log.i(TAG, "[$id] releaseGroup: destroying group resources")
        handler.post {
            try {
                texturePools.clear()
                currentSurface = EGL14.EGL_NO_SURFACE
                if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                isEglInitialized = false
                EGLCoreManager.releaseSharedContext()
            } catch (t: Throwable) {
                Animer.log.e(TAG, "[$id] releaseGroup error: $t")
            } finally {
                handlerThread.quitSafely()
            }
        }
    }
}
