package com.base.animation.gles

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import androidx.annotation.WorkerThread
import com.base.animation.Animer
import com.base.animation.gles.utils.flip
import com.base.animation.gles.utils.rotate
import com.base.animation.gles.utils.scale
import com.base.animation.gles.utils.translate
import com.base.animation.gles.utils.traceSection
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @author zzechao
 * @date 2025/3/19 18:36
 * @description: 渲染器
 * 1. 初始化EGL环境
 * 2. 初始化Shader
 * 3. 初始化纹理
 * 4. 绘制
 */
class EGLRender(private val useSharedContext: Boolean = true) : IRenderer {
    companion object {
        private const val TAG = "EGLRender"
        private const val COORDS_PER_VERTEX: Int = 2
    }


    private val mEGLHelper by lazy { EGLHelper() }
    private val shader by lazy { EGLAnimShader() }

    private var surfaceWidth = -1
    private var surfaceHeight = -1

    /**
     * 画布四个角坐标如下：
     * (-1, 1),(1, 1)
     * (-1,-1),(1,-1)
     */
    private val vertexCoords = floatArrayOf(
        -1.0f, 1.0f,  // 左上
        -1.0f, -1.0f,  // 左下
        1.0f, 1.0f,  // 右上
        1.0f, -1.0f,  // 右下
    )

    /**
     * 画布四个角坐标如下：
     * (0,1),(1,1)
     * (0,0),(1,0)
     */
    private val textureCoords = floatArrayOf(
        0.0f, 1.0f,  // 左上
        0.0f, 0.0f,  // 左下
        1.0f, 1.0f,  // 右上
        1.0f, 0.0f,  // 右下
    )

    // 顶点坐标缓冲区
    private var vertexBuffer: FloatBuffer? = null

    // 纹理坐标缓冲区
    private var textureBuffer: FloatBuffer? = null

    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private var mDisplayScaleX: Float = -1.0f
    private var mDisplayScaleY: Float = -1.0f

    private val mMVPMatrix by lazy { FloatArray(16) }
    private val projection by lazy { FloatArray(16) }
    private val viewMatrix by lazy {
        FloatArray(16).apply {
            Matrix.setLookAtM(this, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
        }
    }
    private val mTexMatrix by lazy { FloatArray(16) }
    private val mIdentityMatrix by lazy {
        FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    }

    // MVP 基础投影视图矩阵缓存（相同尺寸绘制时直接复用，消除重复 orthoM + multiplyMM 损耗）
    private var lastDisplayWidth = -1
    private var lastDisplayHeight = -1
    private var lastSurfaceWidth = -1
    private var lastSurfaceHeight = -1
    private val mBaseVPMatrix = FloatArray(16)
    private val mCurrentMVPMatrix = FloatArray(16)

    // 纹理绑定与着色器模式缓存，消除重复 JNI 绑定与切换损耗
    private var lastBoundTextureId = -1
    private var lastBoundTextureType: EGLAnimTexture.TextureType? = null

    // 帧内缓存：上一次命中的纹理与已上传的 alpha uniform，drawRenderBegin 时重置
    private var lastItemHash = 0
    private var lastItemTexture: EGLAnimTexture? = null
    private var lastAlpha = -1

    /**
     * alpha 为 0 或整个对象在 surface 之外时不需要绘制。
     * 对象在屏幕上的尺寸为 displaySize * scale，以 (x, y) 为中心（与点击判定一致）；
     * 有旋转时按外接圆半径保守判断，并额外留出一个对象尺寸的余量，宁可多画也不误剔除。
     */
    private fun isInvisible(
        displayWidth: Int, displayHeight: Int, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float, rotation: Float
    ): Boolean {
        if (alpha <= 0) return true
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return false
        var halfW = Math.abs(displayWidth * scaleX) / 2f
        var halfH = Math.abs(displayHeight * scaleY) / 2f
        if (rotation != 0f) {
            val r = Math.sqrt((halfW * halfW + halfH * halfH).toDouble()).toFloat()
            halfW = r
            halfH = r
        }
        val marginX = halfW * 2f
        val marginY = halfH * 2f
        return x + halfW + marginX < 0f || x - halfW - marginX > surfaceWidth ||
            y + halfH + marginY < 0f || y - halfH - marginY > surfaceHeight
    }

    private val isReleased = AtomicBoolean(false)
    private val localTexturePools by lazy { EGLTexturePools() }

    private var renderGroup: EGLRenderGroup? = null
    private var mEGLSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun attachGroup(group: EGLRenderGroup) {
        this.renderGroup = group
    }

    private var nanoTime = 0L

    @WorkerThread
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        isReleased.set(false)
        val group = renderGroup
        if (group != null) {
            group.ensureEGLInitialized()
            mEGLSurface = group.createSurface(surface)
            group.makeCurrent(mEGLSurface)
        } else {
            val shareContext = if (useSharedContext) EGLCoreManager.obtainSharedContext() else EGL14.EGL_NO_CONTEXT
            mEGLHelper.initEGL(surface, shareContext)
        }

        // 初始化形状坐标的顶点字节缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexCoords)
        vertexBuffer?.position(0)

        // 初始化纹理坐标顶点字节缓冲区
        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(textureCoords)
        textureBuffer?.position(0)

        shader.initShader()

        refreshSurfaceView(width, height)
    }

    @WorkerThread
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        refreshSurfaceView(width, height)
    }

    @WorkerThread
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

    @WorkerThread
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    private fun refreshSurfaceView(width: Int, height: Int) {
        val group = renderGroup
        if (group != null) {
            group.makeCurrent(mEGLSurface)
        }
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        surfaceWidth = width
        surfaceHeight = height
        if (group != null) {
            group.swapBuffers(mEGLSurface)
        } else {
            mEGLHelper.swapBuffers()
        }
    }

    /**
     * 纹理查找：同一帧内连续相同 hash 的对象（如 20 个红包）直接复用上一次命中的纹理，
     * 跳过两级池的同步锁 + LruCache 查询。
     */
    private fun lookupTexture(hashCode: Int, createTexture: () -> EGLAnimTexture): EGLAnimTexture {
        val last = lastItemTexture
        if (last != null && lastItemHash == hashCode && last.textureId > 0) {
            return last
        }
        val group = renderGroup
        val animTexture = if (group != null) {
            // 同一个工作组内或跨组共享纹理池
            group.texturePools.getTextureIfPresent(hashCode)
                ?: EGLCoreManager.sharedTexturePools.getTextureIfPresent(hashCode)
                ?: run {
                    val created = createTexture()
                    if (created.type == EGLAnimTexture.TextureType.BITMAP) {
                        EGLCoreManager.sharedTexturePools.putTexture(hashCode, created)
                    } else {
                        group.texturePools.putTexture(hashCode, created)
                    }
                    created
                }
        } else if (useSharedContext) {
            // 先在本地池查询（针对私有SurfaceTexture类型的LAYOUT/STRING）
            localTexturePools.getTextureIfPresent(hashCode)
                ?: EGLCoreManager.sharedTexturePools.getTextureIfPresent(hashCode)
                ?: run {
                    val created = createTexture()
                    if (created.type == EGLAnimTexture.TextureType.BITMAP) {
                        EGLCoreManager.sharedTexturePools.putTexture(hashCode, created)
                    } else {
                        localTexturePools.putTexture(hashCode, created)
                    }
                    created
                }
        } else {
            localTexturePools.getTexture(hashCode, createTexture)
        }
        if (animTexture.textureId > 0) {
            lastItemHash = hashCode
            lastItemTexture = animTexture
        }
        return animTexture
    }

    /**
     * @param cullable 仅 BITMAP 类对象传 true：alpha 为 0 或完全在屏幕外时跳过绘制。
     * STRING/LAYOUT 依赖 SurfaceTexture.updateTexImage 消费帧，不能跳过，所以默认 false。
     */
    @WorkerThread
    fun drawItem(
        animId: Long, hashCode: Int, displayWidth: Int, displayHeight: Int, x: Float, y: Float, alpha: Int, scaleX: Float, scaleY: Float, rotation: Float, createTexture: () -> EGLAnimTexture,
        cullable: Boolean = false
    ) {
        if (cullable && isInvisible(displayWidth, displayHeight, x, y, alpha, scaleX, scaleY, rotation)) {
            return
        }
        traceSection("EGL_drawItem") {
            val animTexture = lookupTexture(hashCode, createTexture)

            /**
             * 这里因为画布坐标和纹理坐标存在两倍的缩放比，所以需要将画布坐标和纹理坐标进行缩放，所以将displaySize/2f
             */
            mDisplayScaleX = surfaceWidth / (displayWidth / 2f)
            mDisplayScaleY = surfaceHeight / (displayHeight / 2f)

            val drawX = (x - displayWidth / 4f) / surfaceWidth * mDisplayScaleX
            val drawY = (y - displayHeight / 4f) / surfaceHeight * mDisplayScaleY

            // alpha 连续相同时不重复上传 uniform（drawRenderBegin 会重置）
            if (alpha != lastAlpha) {
                GLES20.glUniform1f(shader.uAlphaHandle, alpha / 255f)
                lastAlpha = alpha
            }

            if (animTexture.type == EGLAnimTexture.TextureType.BITMAP) {
                if (lastBoundTextureType != animTexture.type) {
                    GLES20.glUniform1i(shader.uIsColor2DHandle, 1)
                    GLES20.glUniformMatrix4fv(shader.uTexMatrixHandle, 1, false, mIdentityMatrix, 0)
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                    lastBoundTextureType = animTexture.type
                }
                if (lastBoundTextureId != animTexture.textureId) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, animTexture.textureId)
                    lastBoundTextureId = animTexture.textureId
                }
            } else {
                if (lastBoundTextureType != animTexture.type) {
                    GLES20.glUniform1i(shader.uIsColor2DHandle, 0)
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                    lastBoundTextureType = animTexture.type
                }
                animTexture.surfaceTexture?.let { st ->
                    try {
                        st.updateTexImage()
                        st.getTransformMatrix(mTexMatrix)
                        GLES20.glUniformMatrix4fv(shader.uTexMatrixHandle, 1, false, mTexMatrix, 0)
                    } catch (t: Throwable) {
                    }
                }
                if (lastBoundTextureId != animTexture.textureId) {
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, animTexture.textureId)
                    lastBoundTextureId = animTexture.textureId
                }
            }

            // 复用或更新基础投影视图矩阵（相同尺寸元素直接复用，消除重复 orthoM + multiplyMM 耗时）
            if (displayWidth != lastDisplayWidth || displayHeight != lastDisplayHeight || surfaceWidth != lastSurfaceWidth || surfaceHeight != lastSurfaceHeight) {
                lastDisplayWidth = displayWidth
                lastDisplayHeight = displayHeight
                lastSurfaceWidth = surfaceWidth
                lastSurfaceHeight = surfaceHeight

                Matrix.setIdentityM(mMVPMatrix, 0)
                Matrix.orthoM(projection, 0, -1f, 1f * mDisplayScaleX, -1f * mDisplayScaleY, 1f, 1f, -1f)
                Matrix.multiplyMM(mBaseVPMatrix, 0, projection, 0, viewMatrix, 0)
                mBaseVPMatrix.flip(false, y = true)
            }

            System.arraycopy(mBaseVPMatrix, 0, mCurrentMVPMatrix, 0, 16)
            mCurrentMVPMatrix.translate(drawX, drawY).rotate(rotation).scale(scaleX, scaleY)

            // 将投影和视图变换传递给着色器
            GLES20.glUniformMatrix4fv(shader.vPMatrixHandle, 1, false, mCurrentMVPMatrix, 0)

            // 绘制
            traceSection("EGL_glDrawArrays") {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
        }
    }

    /**
     * 清除颜色并设置全局批次渲染状态
     */
    @WorkerThread
    fun drawRenderBegin() {
        val group = renderGroup
        if (group != null) {
            group.makeCurrent(mEGLSurface)
            group.texturePools.pollAndReleaseEvictedTextures()
            EGLCoreManager.sharedTexturePools.pollAndReleaseEvictedTextures()
        } else {
            if (useSharedContext) {
                EGLCoreManager.sharedTexturePools.pollAndReleaseEvictedTextures()
            }
            localTexturePools.pollAndReleaseEvictedTextures()
        }

        if (surfaceWidth > 0 && surfaceHeight > 0) {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        shader.useShader()

        // 预乘Alpha标准混合模式，在整个绘制批次统一开启
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 为正方形顶点启用控制句柄并传入缓冲区（所有元素通用）
        GLES20.glEnableVertexAttribArray(shader.positionHandle)
        GLES20.glVertexAttribPointer(shader.positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        // 启用纹理坐标控制句柄并传入缓冲区（所有元素通用）
        GLES20.glEnableVertexAttribArray(shader.texCoordinateHandle)
        GLES20.glVertexAttribPointer(shader.texCoordinateHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(shader.texHandle, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glUniform1i(shader.vTextureOESHandle, 1)

        // 重置当前激活纹理单元为GL_TEXTURE0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

        // 重置批次缓存状态
        lastBoundTextureId = -1
        lastBoundTextureType = null
        lastItemHash = 0
        lastItemTexture = null
        lastAlpha = -1
    }

    /**
     * 渲染
     */
    @WorkerThread
    fun drawRenderEnd() {
        GLES20.glDisableVertexAttribArray(shader.positionHandle)
        GLES20.glDisableVertexAttribArray(shader.texCoordinateHandle)
        shader.unUseShader()
        traceSection("EGL_swapBuffers") {
            val group = renderGroup
            if (group != null) {
                group.swapBuffers(mEGLSurface)
            } else {
                mEGLHelper.swapBuffers()
            }
        }
    }

    @WorkerThread
    fun release() {
        if (!isReleased.compareAndSet(false, true)) return
        val group = renderGroup
        if (group != null && mEGLSurface != EGL14.EGL_NO_SURFACE) {
            group.makeCurrent(mEGLSurface)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        shader.destroyShader()
        if (group != null) {
            if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
                group.destroySurface(mEGLSurface)
                mEGLSurface = EGL14.EGL_NO_SURFACE
            }
            renderGroup = null
        } else {
            localTexturePools.clear()
            mEGLHelper.destroyEGL()
            if (useSharedContext) {
                EGLCoreManager.releaseSharedContext()
            }
        }
        vertexBuffer?.clear()
        textureBuffer?.clear()
        lastDisplayWidth = -1
        lastDisplayHeight = -1
        lastSurfaceWidth = -1
        lastSurfaceHeight = -1
        lastBoundTextureId = -1
        lastBoundTextureType = null
        lastItemHash = 0
        lastItemTexture = null
        lastAlpha = -1
    }
}