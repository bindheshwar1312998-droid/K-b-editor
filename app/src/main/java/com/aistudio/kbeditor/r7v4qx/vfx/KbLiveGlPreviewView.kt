package com.aistudio.kbeditor.r7v4qx.vfx

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.aistudio.kbeditor.r7v4qx.model.ColorGradeSpec
import com.aistudio.kbeditor.r7v4qx.model.MaskType
import com.aistudio.kbeditor.r7v4qx.model.VideoClip
import com.aistudio.kbeditor.r7v4qx.model.VideoFxPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class LiveGlParams(
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vignette: Float = 0f,
    val vibrance: Float = 0f,
    val filterIntensity: Float = 1f,
    val chromaEnabled: Boolean = false,
    val keyR: Float = 0f,
    val keyG: Float = 1f,
    val keyB: Float = 0f,
    val chromaSim: Float = 0.35f,
    val fxMode: Float = 0f,
    val fxIntensity: Float = 1f,
    val fxTexture: Float = 0.45f,
    val fxColorShift: Float = 0f,
    val waterReflection: Boolean = false,
    val motionBlur: Float = 0f,
    val beautyGlow: Float = 0f,
    val sharpen: Float = 0f,
    val timeSec: Float = 0f,
    val normTx: Float = 0f,
    val normTy: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotZDeg: Float = 0f,
    val alpha: Float = 1f,
    val wmEnabled: Boolean = false,
    val wmX: Float = 0.8f,
    val wmY: Float = 0.12f,
    val wmW: Float = 0.22f,
    val wmH: Float = 0.08f,
    val eraserFlat: FloatArray = FloatArray(72),
    val eraserCount: Int = 0,
    val maskTypeInt: Int = 0,
    val maskScale: Float = 0.85f,
    val maskScaleX: Float = 1.0f,
    val maskScaleY: Float = 1.0f,
    val maskOffsetX: Float = 0f,
    val maskOffsetY: Float = 0f,
    val maskRotRad: Float = 0f,
    val maskFeather: Float = 0.08f,
    val maskInverted: Boolean = false
)

fun VideoClip.toLiveGlParams(timeSec: Float, fxActive: Boolean): LiveGlParams {
    val g: ColorGradeSpec = colorGrade
    val fi = filterIntensity.coerceIn(0f, 1.5f)
    val fxMode = if (!fxActive) 0f else when (videoFx) {
        VideoFxPreset.NONE -> 0f
        VideoFxPreset.GLITCH_RGB, VideoFxPreset.TEAR_APART, VideoFxPreset.CYBER_GLITCH -> 1f
        VideoFxPreset.VHS_RETRO, VideoFxPreset.OLD_TV, VideoFxPreset.RETRO_FLICKER,
        VideoFxPreset.PC_MONITOR, VideoFxPreset.WINDOWS_XP, VideoFxPreset.TERMINAL_HUD -> 2f
        VideoFxPreset.CAM_SHAKE, VideoFxPreset.EARTHQUAKE, VideoFxPreset.IMPACT_SHAKE -> 3f
        VideoFxPreset.FLASH_STROBE, VideoFxPreset.ACTION_FLASH, VideoFxPreset.WHITE_FLASH -> 4f
        VideoFxPreset.CHROMATIC, VideoFxPreset.PRISM_SHIMMER, VideoFxPreset.SHARDS_LIGHT -> 6f
        VideoFxPreset.ZOOM_PULSE, VideoFxPreset.ZOOM_LENS, VideoFxPreset.HEARTBEAT -> 7f
        VideoFxPreset.CINEMA_GRAIN, VideoFxPreset.BW_GRAIN, VideoFxPreset.BW_SCANNER,
        VideoFxPreset.MONOCHROME_PRO, VideoFxPreset.NOIR_SHADOW -> 8f
        VideoFxPreset.SPLIT_2X, VideoFxPreset.MIRROR_SPLIT -> 9f
        VideoFxPreset.SPLIT_3X, VideoFxPreset.SPLIT_4X, VideoFxPreset.SPLIT_6X, VideoFxPreset.SPLIT_9X -> 10f
        VideoFxPreset.NEON_EDGE, VideoFxPreset.NEON_FLOW, VideoFxPreset.SKETCH_OUTLINE -> 11f
        else -> when (videoFx.category) {
            "Split & Glitch" -> 6f
            "Retro & Film" -> 2f
            "Cinema & Moody" -> 8f
            else -> 5f
        }
    }
    val isWater = fxActive && videoFx in listOf(
        VideoFxPreset.WATER_REFLECTION,
        VideoFxPreset.WATER_RIPPLE,
        VideoFxPreset.RAIN_PUDDLE,
        VideoFxPreset.MIRROR_LAKE
    )
    val (kr, kg, kb) = when (chromaKey.keyHue.lowercase()) {
        "blue" -> Triple(0.05f, 0.25f, 0.95f)
        "red" -> Triple(0.95f, 0.12f, 0.12f)
        else -> Triple(0.08f, 0.92f, 0.15f)
    }
    return LiveGlParams(
        exposure = g.brightness * fi,
        contrast = 1f + (g.contrast - 1f) * fi,
        saturation = 1f + (g.saturation - 1f) * fi,
        warmth = g.warmth * fi,
        highlights = g.highlights * fi,
        shadows = g.shadows * fi,
        vignette = g.vignette * fi,
        vibrance = g.vibrance * fi,
        wmEnabled = watermarkRemover.enabled && !watermarkRemover.cropMode,
        wmX = watermarkRemover.posX,
        wmY = watermarkRemover.posY,
        wmW = watermarkRemover.boxWidth,
        wmH = watermarkRemover.boxHeight,
        eraserFlat = FloatArray(72).also { arr ->
            magicEraserPoints.take(24).forEachIndexed { idx, pt ->
                arr[idx * 3] = pt.x
                arr[idx * 3 + 1] = pt.y
                arr[idx * 3 + 2] = pt.radius
            }
        },
        eraserCount = magicEraserPoints.size.coerceAtMost(24),
        filterIntensity = fi,
        chromaEnabled = chromaKey.enabled,
        keyR = kr,
        keyG = kg,
        keyB = kb,
        chromaSim = chromaKey.tolerance.coerceIn(0.05f, 0.8f),
        fxMode = fxMode,
        fxIntensity = videoFxIntensity.coerceIn(0.1f, 10f),
        fxTexture = videoFxTexture.coerceIn(0f, 1f),
        fxColorShift = videoFxColorShift.coerceIn(-1f, 1f),
        waterReflection = isWater,
        motionBlur = if (fxActive && videoFx in listOf(
                VideoFxPreset.MOTION_BLUR, VideoFxPreset.RADIAL_BLUR,
                VideoFxPreset.TURBO_BLUR, VideoFxPreset.HALO_BLUR
            )
        ) 0.012f * videoFxIntensity.coerceIn(0.1f, 10f) else 0f,
        beautyGlow = (g.beautyGlow + g.beautySmooth * 0.4f + if (fxActive && videoFx in listOf(
                VideoFxPreset.SOFT_GLOW, VideoFxPreset.DREAM_BLOOM,
                VideoFxPreset.PEARL_GLOW, VideoFxPreset.PORTRAIT_FOCUS
            )
        ) 0.35f * videoFxIntensity.coerceIn(0.1f, 10f) else 0f).coerceIn(0f, 0.85f),
        sharpen = when (g.lutName) {
            "4K Ultra Crisp" -> 0.55f
            "HD Crisp" -> 0.38f
            else -> if (fxActive && videoFx in listOf(VideoFxPreset.SILK_SHARPEN, VideoFxPreset.CRISP_HDR, VideoFxPreset.MACRO_SHARPEN)) 0.48f else 0f
        },
        timeSec = timeSec,
        maskTypeInt = when (maskType) {
            MaskType.NONE -> 0
            MaskType.CIRCLE -> 1
            MaskType.ROUNDED_RECT -> 2
            MaskType.SPLIT_WIPE -> 3
            MaskType.MIRROR -> 4
            MaskType.HEART -> 5
            MaskType.STAR -> 6
            MaskType.ANAMORPHIC_BARS -> 7
        },
        maskScale = maskScale.coerceIn(0.15f, 2.5f),
        maskScaleX = maskScaleX.coerceIn(0.25f, 2.5f),
        maskScaleY = maskScaleY.coerceIn(0.25f, 2.5f),
        maskOffsetX = maskOffsetX.coerceIn(-0.8f, 0.8f),
        maskOffsetY = maskOffsetY.coerceIn(-0.8f, 0.8f),
        maskRotRad = Math.toRadians(maskRotationDeg.toDouble()).toFloat(),
        maskFeather = maskFeather.coerceIn(0.002f, 0.45f),
        maskInverted = maskInverted
    )
}

@Composable
fun KbLiveGlPreviewView(
    exoPlayer: ExoPlayer,
    params: LiveGlParams,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val renderer = remember { KbLiveGlRenderer() }
    var glViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, glViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(params) {
        renderer.params = params
        glViewRef?.requestRender()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    renderer.setVideoSize(videoSize.width, videoSize.height)
                    glViewRef?.requestRender()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            renderer.detachFromPlayer(exoPlayer)
        }
    }

    AndroidView(
        factory = { ctx: Context ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                preserveEGLContextOnPause = true
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                renderer.onSurfaceReady = { surface ->
                    post {
                        exoPlayer.setVideoSurface(surface)
                        requestRender()
                    }
                }
                renderer.onFrameArrived = {
                    requestRender()
                }
                glViewRef = this
            }
        },
        update = { view ->
            glViewRef = view
            renderer.params = params
            val desiredMode = if (isPlaying) GLSurfaceView.RENDERMODE_CONTINUOUSLY else GLSurfaceView.RENDERMODE_WHEN_DIRTY
            if (view.renderMode != desiredMode) {
                view.renderMode = desiredMode
            }
            if (!isPlaying) {
                view.requestRender()
            }
        },
        modifier = modifier
    )
}

private class KbLiveGlRenderer : GLSurfaceView.Renderer {

    @Volatile
    var params: LiveGlParams = LiveGlParams()

    var onSurfaceReady: ((Surface) -> Unit)? = null
    var onFrameArrived: (() -> Unit)? = null

    private var programId = 0
    private var oesTexId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null

    @Volatile private var viewW = 1
    @Volatile private var viewH = 1
    @Volatile private var vidW = 16
    @Volatile private var vidH = 9

    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val quadCoords = floatArrayOf(
        -1f, -1f, 0f, 0f,
         1f, -1f, 1f, 0f,
        -1f,  1f, 0f, 1f,
         1f,  1f, 1f, 1f
    )
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(quadCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(quadCoords)
        .apply { position(0) }

    fun setVideoSize(w: Int, h: Int) {
        vidW = w.coerceAtLeast(1)
        vidH = h.coerceAtLeast(1)
    }

    fun detachFromPlayer(player: ExoPlayer) {
        videoSurface?.let { s ->
            runCatching { player.clearVideoSurface(s) }
            runCatching { s.release() }
        }
        runCatching { surfaceTexture?.release() }
        videoSurface = null
        surfaceTexture = null
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTexId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        programId = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        runCatching { videoSurface?.release() }
        runCatching { surfaceTexture?.release() }
        val st = SurfaceTexture(oesTexId)
        st.setOnFrameAvailableListener {
            onFrameArrived?.invoke()
        }
        surfaceTexture = st
        val surf = Surface(st)
        videoSurface = surf
        onSurfaceReady?.invoke(surf)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewW = width.coerceAtLeast(1)
        viewH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewW, viewH)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (programId == 0) return
        runCatching { surfaceTexture?.updateTexImage() }
        runCatching { surfaceTexture?.getTransformMatrix(stMatrix) }
            ?: Matrix.setIdentityM(stMatrix, 0)

        val p = params
        // Compute aspect-fill (center crop) + clip/animation/keyframe MVP matrix directly in GL
        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.translateM(mvpMatrix, 0, p.normTx * 2f, -p.normTy * 2f, 0f)
        if (kotlin.math.abs(p.rotZDeg) > 0.01f) {
            Matrix.rotateM(mvpMatrix, 0, -p.rotZDeg, 0f, 0f, 1f)
        }
        Matrix.scaleM(mvpMatrix, 0, p.scaleX, p.scaleY, 1f)
        val viewAspect = viewW.toFloat() / viewH.toFloat()
        val vidAspect = vidW.toFloat() / vidH.toFloat()
        if (vidAspect > viewAspect) {
            Matrix.scaleM(mvpMatrix, 0, vidAspect / viewAspect, 1f, 1f)
        } else {
            Matrix.scaleM(mvpMatrix, 0, 1f, viewAspect / vidAspect, 1f)
        }

        GLES20.glClearColor(0.04f, 0.05f, 0.08f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(programId)

        val aPos = GLES20.glGetAttribLocation(programId, "aPos")
        val aTex = GLES20.glGetAttribLocation(programId, "aTex")
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aTex)
        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(programId, "uMvpMatrix"), 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(programId, "uStMatrix"), 1, false, stMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programId, "uTex"), 0)

        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uExposure"), p.exposure)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uContrast"), p.contrast)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uSaturation"), p.saturation)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uWarmth"), p.warmth)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uHighlights"), p.highlights)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uShadows"), p.shadows)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uVignette"), p.vignette)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uVibrance"), p.vibrance)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uChromaEnabled"), if (p.chromaEnabled) 1f else 0f)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(programId, "uKeyColor"), p.keyR, p.keyG, p.keyB)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uChromaSim"), p.chromaSim)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFxMode"), p.fxMode)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFxIntensity"), p.fxIntensity)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFxTexture"), p.fxTexture)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uFxColorShift"), p.fxColorShift)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uWaterMode"), if (p.waterReflection) 1f else 0f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uMotionBlur"), p.motionBlur)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uBeautyGlow"), p.beautyGlow)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uSharpen"), p.sharpen)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uTime"), p.timeSec)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uAlpha"), p.alpha.coerceIn(0f, 1f))
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uWmEnabled"), if (p.wmEnabled) 1f else 0f)
        GLES20.glUniform4f(GLES20.glGetUniformLocation(programId, "uWmRect"), p.wmX, p.wmY, p.wmW, p.wmH)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uEraserCount"), p.eraserCount.toFloat())
        if (p.eraserCount > 0) {
            GLES20.glUniform3fv(GLES20.glGetUniformLocation(programId, "uEraserPts"), 24, p.eraserFlat, 0)
        }
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uMaskType"), p.maskTypeInt.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uMaskScale"), p.maskScale)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(programId, "uMaskScaleXY"), p.maskScaleX, p.maskScaleY)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(programId, "uMaskOffset"), p.maskOffsetX, p.maskOffsetY)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uMaskRot"), p.maskRotRad)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uMaskFeather"), p.maskFeather)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(programId, "uMaskInv"), if (p.maskInverted) 1f else 0f)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTex)
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        if (vShader == 0 || fShader == 0) return 0
        val prog = GLES20.glCreateProgram()
        if (prog == 0) return 0
        GLES20.glAttachShader(prog, vShader)
        GLES20.glAttachShader(prog, fShader)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        if (s == 0) return 0
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(s)
            return 0
        }
        return s
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPos;
            attribute vec4 aTex;
            uniform mat4 uMvpMatrix;
            uniform mat4 uStMatrix;
            varying vec2 vTexCoord;
            varying vec2 vScreenUv;
            void main() {
                gl_Position = uMvpMatrix * aPos;
                vTexCoord = (uStMatrix * vec4(aTex.xy, 0.0, 1.0)).xy;
                vScreenUv = aTex.xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            uniform samplerExternalOES uTex;
            varying vec2 vTexCoord;
            varying vec2 vScreenUv;

            uniform float uExposure;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uWarmth;
            uniform float uHighlights;
            uniform float uShadows;
            uniform float uVignette;
            uniform float uVibrance;

            uniform float uChromaEnabled;
            uniform vec3 uKeyColor;
            uniform float uChromaSim;

            uniform float uFxMode;
            uniform float uFxIntensity;
            uniform float uFxTexture;
            uniform float uFxColorShift;
            uniform float uWaterMode;
            uniform float uMotionBlur;
            uniform float uBeautyGlow;
            uniform float uSharpen;
            uniform float uTime;
            uniform float uAlpha;
            uniform float uWmEnabled;
            uniform vec4 uWmRect;
            uniform float uEraserCount;
            uniform vec3 uEraserPts[24];
            uniform float uMaskType;
            uniform float uMaskScale;
            uniform vec2 uMaskScaleXY;
            uniform vec2 uMaskOffset;
            uniform float uMaskRot;
            uniform float uMaskFeather;
            uniform float uMaskInv;

            float rand2(vec2 co) {
                return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
            }

            void main() {
                vec2 uv = vTexCoord;

                // GPU Water Reflection & Ripple
                if (uWaterMode > 0.5 && vScreenUv.y < 0.38) {
                    float wave = sin(vScreenUv.y * 55.0 + uTime * 6.0) * 0.012 * uFxIntensity;
                    uv.x = clamp(uv.x + wave, 0.0, 1.0);
                    uv.y = clamp(1.0 - uv.y, 0.0, 1.0);
                }

                // Split FX (2-Split / Multi-Split)
                if (uFxMode > 8.5 && uFxMode < 9.5) {
                    uv.y = fract(uv.y * 2.0);
                } else if (uFxMode > 9.5 && uFxMode < 10.5) {
                    uv = fract(uv * 2.0);
                }

                // Shake / Glitch UV offsets
                if (uFxMode > 0.5 && uFxMode < 1.5) {
                    float slice = floor(uv.y * 18.0);
                    float glitch = (rand2(vec2(slice, floor(uTime * 15.0))) - 0.5) * 0.05 * uFxIntensity;
                    uv.x = clamp(uv.x + glitch, 0.0, 1.0);
                } else if (uFxMode > 2.5 && uFxMode < 3.5) {
                    uv.x = clamp(uv.x + sin(uTime * 35.0) * 0.015 * uFxIntensity, 0.0, 1.0);
                    uv.y = clamp(uv.y + cos(uTime * 29.0) * 0.012 * uFxIntensity, 0.0, 1.0);
                } else if (uFxMode > 6.5 && uFxMode < 7.5) {
                    float z = 1.0 - (0.08 * abs(sin(uTime * 8.0)) * uFxIntensity);
                    uv = (uv - 0.5) * z + 0.5;
                }

                vec4 col = texture2D(uTex, uv);
                if (uWmEnabled > 0.5) {
                    float dx = abs(vScreenUv.x - uWmRect.x);
                    float dy = abs((1.0 - vScreenUv.y) - uWmRect.y);
                    float hw = max(uWmRect.z * 0.5, 0.02);
                    float hh = max(uWmRect.w * 0.5, 0.02);
                    if (dx < hw && dy < hh) {
                        vec2 offY = vec2(0.0, hh * 1.15);
                        vec2 offX = vec2(hw * 0.55, 0.0);
                        vec4 s1 = texture2D(uTex, clamp(uv + offY, 0.0, 1.0));
                        vec4 s2 = texture2D(uTex, clamp(uv - offY, 0.0, 1.0));
                        vec4 s3 = texture2D(uTex, clamp(uv + offX, 0.0, 1.0));
                        vec4 s4 = texture2D(uTex, clamp(uv - offX, 0.0, 1.0));
                        vec4 cleanPatch = (s1 + s2 + s3 + s4) * 0.25;
                        float edge = smoothstep(hw, hw * 0.65, dx) * smoothstep(hh, hh * 0.65, dy);
                        col = mix(col, cleanPatch, edge);
                    }
                }
                if (uEraserCount > 0.5) {
                    vec2 screenPt = vec2(vScreenUv.x, 1.0 - vScreenUv.y);
                    for (int i = 0; i < 24; i++) {
                        if (float(i) < uEraserCount) {
                        vec3 ep = uEraserPts[i];
                        float dist = distance(screenPt, ep.xy);
                        float rad = max(ep.z, 0.055);
                        if (dist < rad) {
                            float off = rad * 1.45;
                            vec4 e1 = texture2D(uTex, clamp(uv + vec2(off, 0.0), 0.0, 1.0));
                            vec4 e2 = texture2D(uTex, clamp(uv - vec2(off, 0.0), 0.0, 1.0));
                            vec4 e3 = texture2D(uTex, clamp(uv + vec2(0.0, off), 0.0, 1.0));
                            vec4 e4 = texture2D(uTex, clamp(uv - vec2(0.0, off), 0.0, 1.0));
                            vec4 e5 = texture2D(uTex, clamp(uv + vec2(off, off) * 0.7, 0.0, 1.0));
                            vec4 e6 = texture2D(uTex, clamp(uv + vec2(-off, off) * 0.7, 0.0, 1.0));
                            vec4 e7 = texture2D(uTex, clamp(uv + vec2(off, -off) * 0.7, 0.0, 1.0));
                            vec4 e8 = texture2D(uTex, clamp(uv - vec2(off, off) * 0.7, 0.0, 1.0));
                            vec4 clean = (e1 + e2 + e3 + e4 + e5 + e6 + e7 + e8) * 0.125;
                            float blend = smoothstep(rad, rad * 0.25, dist);
                            col = mix(col, clean, blend);
                        }
                        }
                    }
                }
                if (uMotionBlur > 0.001) {
                    vec4 b1 = texture2D(uTex, clamp(uv + vec2(uMotionBlur, 0.0), 0.0, 1.0));
                    vec4 b2 = texture2D(uTex, clamp(uv - vec2(uMotionBlur, 0.0), 0.0, 1.0));
                    col = (col + b1 + b2) / 3.0;
                }
                if (uBeautyGlow > 0.01) {
                    vec3 g1 = texture2D(uTex, clamp(uv + vec2(0.004, 0.004), 0.0, 1.0)).rgb;
                    vec3 g2 = texture2D(uTex, clamp(uv - vec2(0.004, 0.004), 0.0, 1.0)).rgb;
                    col.rgb = mix(col.rgb, (col.rgb + g1 + g2) / 3.0 + vec3(0.04), uBeautyGlow);
                }
                if (uSharpen > 0.01) {
                    vec3 n1 = texture2D(uTex, clamp(uv + vec2(0.0025, 0.0), 0.0, 1.0)).rgb;
                    vec3 n2 = texture2D(uTex, clamp(uv - vec2(0.0025, 0.0), 0.0, 1.0)).rgb;
                    vec3 n3 = texture2D(uTex, clamp(uv + vec2(0.0, 0.0025), 0.0, 1.0)).rgb;
                    vec3 n4 = texture2D(uTex, clamp(uv - vec2(0.0, 0.0025), 0.0, 1.0)).rgb;
                    vec3 avg = (n1 + n2 + n3 + n4) * 0.25;
                    col.rgb = clamp(col.rgb + (col.rgb - avg) * uSharpen, 0.0, 1.0);
                }

                // Chromatic Aberration / RGB Glitch
                if ((uFxMode > 0.5 && uFxMode < 1.5) || (uFxMode > 5.5 && uFxMode < 6.5)) {
                    float off = 0.012 * uFxIntensity;
                    col.r = texture2D(uTex, clamp(uv + vec2(off, 0.0), 0.0, 1.0)).r;
                    col.b = texture2D(uTex, clamp(uv - vec2(off, 0.0), 0.0, 1.0)).b;
                }

                // Neon Edge
                if (uFxMode > 10.5) {
                    vec3 cR = texture2D(uTex, clamp(uv + vec2(0.004, 0.0), 0.0, 1.0)).rgb;
                    vec3 cU = texture2D(uTex, clamp(uv + vec2(0.0, 0.004), 0.0, 1.0)).rgb;
                    float edge = length(col.rgb - cR) + length(col.rgb - cU);
                    col.rgb = mix(col.rgb * 0.45, vec3(0.13, 0.83, 0.93) * edge * 4.0 * uFxIntensity, 0.75);
                }

                // CapCut-style Chroma Key (Chroma-Dominance + Spill Suppression)
                if (uChromaEnabled > 0.5) {
                    float dom = 0.0;
                    if (uKeyColor.g > uKeyColor.r && uKeyColor.g > uKeyColor.b) {
                        dom = col.g - max(col.r, col.b);
                    } else if (uKeyColor.b > uKeyColor.r && uKeyColor.b > uKeyColor.g) {
                        dom = col.b - max(col.r, col.g);
                    } else {
                        dom = col.r - max(col.g, col.b);
                    }
                    float thresh = clamp(0.42 - uChromaSim * 0.48, 0.02, 0.35);
                    float keyMask = smoothstep(thresh + 0.10, thresh - 0.04, dom);
                    if (uKeyColor.g > uKeyColor.r) {
                        col.g = min(col.g, max(col.r, col.b) * 1.04 + 0.02);
                    }
                    col.rgb = mix(vec3(0.04, 0.06, 0.10), col.rgb, keyMask);
                }

                // Color Grading (Exposure, Contrast, Saturation, Warmth, Shadows/Highlights)
                col.rgb *= pow(2.0, uExposure);
                col.rgb = (col.rgb - 0.5) * uContrast + 0.5;
                float luma = dot(col.rgb, vec3(0.299, 0.587, 0.114));
                col.rgb = mix(vec3(luma), col.rgb, uSaturation);
                if (abs(uVibrance) > 0.01) {
                    float maxC = max(col.r, max(col.g, col.b));
                    float minC = min(col.r, min(col.g, col.b));
                    float satCur = maxC - minC;
                    float skinMask = clamp(1.0 - abs(col.r - col.g) * 2.2, 0.25, 1.0);
                    float vibAmt = uVibrance * (1.0 - satCur) * skinMask;
                    col.rgb = mix(vec3(luma), col.rgb, 1.0 + vibAmt);
                }
                col.r += uWarmth * 0.18;
                col.b -= uWarmth * 0.18;
                col.rgb += (1.0 - luma) * uShadows * 0.25;
                col.rgb += luma * uHighlights * 0.25;

                // B&W / Cinema Grain
                if (uFxMode > 7.5 && uFxMode < 8.5) {
                    float gLuma = dot(col.rgb, vec3(0.299, 0.587, 0.114));
                    col.rgb = mix(col.rgb, vec3(gLuma), 0.65 * uFxIntensity);
                }

                // VHS Scanlines & FX Texture Grain
                if (uFxMode > 1.5 && uFxMode < 2.5) {
                    float scan = sin(vScreenUv.y * 240.0) * 0.08 * uFxIntensity;
                    col.rgb -= scan;
                }
                if (uFxMode > 0.5 && uFxTexture > 0.05) {
                    float grain = (rand2(vScreenUv + fract(uTime)) - 0.5) * 0.18 * uFxTexture;
                    col.rgb += grain;
                }

                // FX Color Shift
                if (uFxMode > 0.5 && abs(uFxColorShift) > 0.04) {
                    vec3 tint = uFxColorShift > 0.0 ? vec3(1.0, 0.45, 0.15) : vec3(0.15, 0.85, 1.0);
                    col.rgb = mix(col.rgb, tint * max(luma, 0.2), abs(uFxColorShift) * 0.35);
                }

                // Water blue tint on lower reflection
                if (uWaterMode > 0.5 && vScreenUv.y < 0.38) {
                    col.rgb = mix(col.rgb, vec3(0.12, 0.68, 0.85), 0.28);
                }

                // Vignette
                if (uVignette > 0.01) {
                    float dist = distance(vScreenUv, vec2(0.5, 0.5));
                    col.rgb *= smoothstep(0.95, 0.35 - uVignette * 0.2, dist);
                }

                // General FX / Flash pulse
                if (uFxMode > 3.5 && uFxMode < 4.5) {
                    float flash = 0.25 * max(0.0, sin(uTime * 18.0)) * uFxIntensity;
                    col.rgb += vec3(flash);
                } else if (uFxMode > 4.5 && uFxMode < 5.5) {
                    float glow = 0.08 * (0.5 + 0.5 * sin(uTime * 6.0)) * uFxIntensity;
                    col.rgb += vec3(glow);
                }

                vec3 finalRgb = mix(vec3(0.03, 0.04, 0.06), clamp(col.rgb, 0.0, 1.0), clamp(uAlpha, 0.0, 1.0));
                if (uMaskType > 0.5) {
                    vec2 mp = vec2(vScreenUv.x, 1.0 - vScreenUv.y) - (vec2(0.5) + uMaskOffset);
                    float cs = cos(uMaskRot);
                    float sn = sin(uMaskRot);
                    vec2 rmp = vec2(mp.x * cs + mp.y * sn, -mp.x * sn + mp.y * cs);
                    vec2 smp = rmp / max(uMaskScale * uMaskScaleXY, vec2(0.08));
                    float mDist = 0.0;
                    if (uMaskType < 1.5) {
                        mDist = length(smp * vec2(1.0, 1.35)) - 0.42;
                    } else if (uMaskType < 2.5) {
                        vec2 q = abs(smp) - vec2(0.36, 0.36);
                        mDist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - 0.05;
                    } else if (uMaskType < 3.5) {
                        mDist = rmp.x;
                    } else if (uMaskType < 4.5) {
                        mDist = abs(smp.y) - 0.28;
                    } else if (uMaskType < 7.5) {
                        mDist = length(smp * vec2(1.0, 1.25)) - 0.42;
                    }
                    float feather = max(uMaskFeather, 0.005);
                    float maskAlpha = 1.0 - smoothstep(-feather, feather, mDist);
                    if (uMaskInv > 0.5) maskAlpha = 1.0 - maskAlpha;
                    col.rgb *= maskAlpha;
                }
                gl_FragColor = vec4(finalRgb, 1.0);
            }
        """
    }
}
