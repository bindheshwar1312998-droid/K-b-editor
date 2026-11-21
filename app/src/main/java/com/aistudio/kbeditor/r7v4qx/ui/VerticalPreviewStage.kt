package com.aistudio.kbeditor.r7v4qx.ui

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.aistudio.kbeditor.r7v4qx.audio.StudioAudioDspProcessor
import com.aistudio.kbeditor.r7v4qx.media.DeviceEngineProfile
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.aistudio.kbeditor.r7v4qx.R
import com.aistudio.kbeditor.r7v4qx.model.*
import com.aistudio.kbeditor.r7v4qx.ui.theme.*
import com.aistudio.kbeditor.r7v4qx.vfx.ChromaKeyGlEffect
import com.aistudio.kbeditor.r7v4qx.vfx.CinematicGradeGlEffect
import com.aistudio.kbeditor.r7v4qx.vfx.VideoFxGlEffect
import com.aistudio.kbeditor.r7v4qx.vfx.KbLiveGlPreviewView
import com.aistudio.kbeditor.r7v4qx.vfx.toLiveGlParams
import com.aistudio.kbeditor.r7v4qx.vfx.CurveEvaluators
import com.aistudio.kbeditor.r7v4qx.vision.AiAutoCutEngine
import com.aistudio.kbeditor.r7v4qx.vision.DetectedFaceBox
import com.aistudio.kbeditor.r7v4qx.vision.FaceSwapEngine
import com.aistudio.kbeditor.r7v4qx.vision.PreparedSourceFace
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun VerticalPreviewStage(
    project: KbEditorProject,
    playheadMs: Long,
    isPlaying: Boolean,
    selectedClip: VideoClip?,
    clipLocalMs: Long = playheadMs,
    isMotionTrackerMode: Boolean = false,
    isMaskEditMode: Boolean = false,
    isMagicEraserMode: Boolean = false,
    onPaintEraser: (Float, Float) -> Unit = { _, _ -> },
    onTrackerTap: (Float, Float) -> Unit = { _, _ -> },
    onTransformMask: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onDragCaption: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDragPip: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDragWatermark: (Float, Float) -> Unit = { _, _ -> },
    onTransformVideo: (Float, Float, Float) -> Unit = { _, _, _ -> },
    onTransformPip: (String, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onTransformCaption: (String, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    abBypassNoiseReduction: Boolean = false,
    selectedPipId: String? = null,
    onSelectPip: (String?) -> Unit = {},
    onDeletePip: (String) -> Unit = {},
    onResetTransform: () -> Unit = {},
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ratio = project.aspectRatio.widthRatio / project.aspectRatio.heightRatio
    val colorMatrix = selectedClip?.let { CurveEvaluators.buildGradeColorMatrix(it.colorGrade) }

    val clipNormT = if (selectedClip != null && selectedClip.activeDurationMs > 0L) {
        (clipLocalMs.toFloat() / selectedClip.activeDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val kf = selectedClip?.let { CurveEvaluators.interpolateKeyframes(it.keyframes, clipNormT) }
    val trackPt = selectedClip?.motionTrackPoints?.takeIf { it.isNotEmpty() }?.let {
        CurveEvaluators.interpolateKeyframes(it, clipNormT)
    }

    val maskRotDeg = selectedClip?.maskRotationDeg ?: 0f
    val maskScale = (selectedClip?.maskScale ?: 0.85f).coerceIn(0.15f, 2.5f)
    val maskScaleX = (selectedClip?.maskScaleX ?: 1.0f).coerceIn(0.25f, 2.5f)
    val maskScaleY = (selectedClip?.maskScaleY ?: 1.0f).coerceIn(0.25f, 2.5f)
    val maskOffX = selectedClip?.maskOffsetX ?: 0f
    val maskOffY = selectedClip?.maskOffsetY ?: 0f
    val maskInv = selectedClip?.maskInverted == true
    val maskShape = when (selectedClip?.maskType) {
        MaskType.CIRCLE, MaskType.ROUNDED_RECT, MaskType.SPLIT_WIPE, MaskType.MIRROR, MaskType.HEART, MaskType.STAR -> GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val p = Path()
            when (selectedClip.maskType) {
                MaskType.CIRCLE -> {
                    val rad = size.minDimension * 0.48f
                    p.addOval(androidx.compose.ui.geometry.Rect(cx - rad, cy - rad, cx + rad, cy + rad))
                }
                MaskType.ROUNDED_RECT -> {
                    p.addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            w * 0.08f, h * 0.14f, w * 0.92f, h * 0.86f,
                            androidx.compose.ui.geometry.CornerRadius(42f, 42f)
                        )
                    )
                }
                MaskType.SPLIT_WIPE -> {
                    p.addRect(androidx.compose.ui.geometry.Rect(-w * 0.5f, -h * 0.5f, w * 0.5f, h * 1.5f))
                }
                MaskType.MIRROR -> {
                    p.addRect(androidx.compose.ui.geometry.Rect(-w * 0.5f, h * 0.22f, w * 1.5f, h * 0.78f))
                }
                MaskType.HEART -> {
                    p.moveTo(w * 0.5f, h * 0.88f)
                    p.cubicTo(w * 0.08f, h * 0.58f, w * 0.05f, h * 0.20f, w * 0.30f, h * 0.16f)
                    p.cubicTo(w * 0.44f, h * 0.14f, w * 0.50f, h * 0.28f, w * 0.50f, h * 0.32f)
                    p.cubicTo(w * 0.50f, h * 0.28f, w * 0.56f, h * 0.14f, w * 0.70f, h * 0.16f)
                    p.cubicTo(w * 0.95f, h * 0.20f, w * 0.92f, h * 0.58f, w * 0.5f, h * 0.88f)
                    p.close()
                }
                MaskType.STAR -> {
                    val outer = size.minDimension * 0.47f
                    val inner = size.minDimension * 0.20f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outer else inner
                        val angle = Math.toRadians((i * 36 - 90).toDouble())
                        val x = cx + (r * kotlin.math.cos(angle)).toFloat()
                        val y = cy + (r * kotlin.math.sin(angle)).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                }
                else -> Unit
            }
            val ap = p.asAndroidPath()
            val tx = maskOffX * w
            val ty = maskOffY * h
            val m = android.graphics.Matrix().apply {
                if (selectedClip.maskType != MaskType.SPLIT_WIPE && selectedClip.maskType != MaskType.MIRROR) {
                    postScale(maskScale * maskScaleX, maskScale * maskScaleY, cx, cy)
                }
                postTranslate(tx, ty)
                postRotate(maskRotDeg, cx + tx, cy + ty)
            }
            ap.transform(m)
            if (maskInv) {
                val full = android.graphics.Path().apply { addRect(0f, 0f, w, h, android.graphics.Path.Direction.CW) }
                full.op(ap, android.graphics.Path.Op.DIFFERENCE)
                addPath(full.asComposePath())
            } else {
                addPath(ap.asComposePath())
            }
        }
        else -> RoundedCornerShape(0.dp)
    }

    val mediaUri = selectedClip?.proxyUri?.takeIf { it.isNotBlank() } ?: selectedClip?.mediaUri.orEmpty()
    val isImageClip = remember(mediaUri, selectedClip?.isAiGeneratedBroll) {
        if (mediaUri.isBlank()) false
        else if (selectedClip?.isAiGeneratedBroll == true) true
        else if (mediaUri.endsWith(".png", true) || mediaUri.endsWith(".jpg", true) || mediaUri.endsWith(".jpeg", true) || mediaUri.endsWith(".webp", true)) true
        else runCatching { context.contentResolver.getType(Uri.parse(mediaUri))?.startsWith("image/") == true }.getOrDefault(false)
    }

    val liveAudioDsp = remember { StudioAudioDspProcessor() }

    val renderersFactory = remember(context) {
        object : DefaultRenderersFactory(context) {
            init {
                setEnableDecoderFallback(true)
            }
            override fun buildAudioSink(
                ctx: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(ctx)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(liveAudioDsp))
                    .build()
            }
        }
    }

    val exoPlayer = remember(context) {
        val tuning = DeviceEngineProfile.activeTuning
        val fastLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(tuning.minBufferMs, tuning.maxBufferMs, tuning.playbackBufferMs, tuning.rebufferMs)
            .build()
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(fastLoadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
            }
    }

    val audioPlayer = remember(context) {
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    val sfxPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    var lastTriggeredSfxId by remember { mutableStateOf<String?>(null) }

    val aiAutoCutEngine = remember { AiAutoCutEngine() }
    val faceSwapEngine = remember(context) { FaceSwapEngine(context) }
    var sourceFaceProfile by remember { mutableStateOf<PreparedSourceFace?>(null) }
    var faceSwapOverlayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var cutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(exoPlayer) {
        var errorRetries = 0
        val recoveryListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    errorRetries = 0
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (errorRetries < 2 && exoPlayer.mediaItemCount > 0) {
                    errorRetries++
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(pos)
                    exoPlayer.playWhenReady = isPlaying
                }
            }
        }
        exoPlayer.addListener(recoveryListener)
        onDispose {
            exoPlayer.removeListener(recoveryListener)
            exoPlayer.release()
            audioPlayer.release()
            sfxPlayer.release()
            aiAutoCutEngine.close()
            faceSwapEngine.close()
        }
    }

    var loadedMediaUri by remember { mutableStateOf("") }
    LaunchedEffect(selectedClip?.id, mediaUri, isImageClip) {
        if (mediaUri.isNotBlank() && !isImageClip) {
            val targetPos = (selectedClip?.trimStartMs ?: 0L) + clipLocalMs
            if (loadedMediaUri != mediaUri) {
                loadedMediaUri = mediaUri
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUri)))
                exoPlayer.prepare()
                exoPlayer.seekTo(targetPos)
            } else if (!isPlaying && abs(exoPlayer.currentPosition - targetPos) > 120L) {
                exoPlayer.seekTo(targetPos)
            } else if (isPlaying && abs(exoPlayer.currentPosition - targetPos) > 450L) {
                exoPlayer.seekTo(targetPos)
            }
            exoPlayer.playWhenReady = isPlaying
        } else {
            loadedMediaUri = ""
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(isPlaying, mediaUri) {
        if (mediaUri.isNotBlank() && !isImageClip) {
            exoPlayer.playWhenReady = isPlaying
        }
    }

    LaunchedEffect(clipLocalMs, isPlaying, mediaUri) {
        if (mediaUri.isNotBlank() && !isImageClip) {
            val targetPos = (selectedClip?.trimStartMs ?: 0L) + clipLocalMs
            if (!isPlaying && abs(exoPlayer.currentPosition - targetPos) > 120L) {
                exoPlayer.seekTo(targetPos)
            } else if (isPlaying && abs(exoPlayer.currentPosition - targetPos) > 2500L) {
                exoPlayer.seekTo(targetPos)
            }
        }
    }

    val effectiveSpeed = remember(selectedClip?.speedMultiplier, selectedClip?.speedRamp, clipNormT) {
        if (selectedClip == null) {
            1f
        } else if (selectedClip.speedRamp != SpeedRampPreset.CONSTANT) {
            CurveEvaluators.evaluateSpeedMultiplier(selectedClip.speedRamp, clipNormT).coerceIn(0.25f, 4.0f)
        } else {
            selectedClip.speedMultiplier.coerceIn(0.1f, 50.0f)
        }
    }

    LaunchedEffect(effectiveSpeed, mediaUri, isImageClip) {
        if (mediaUri.isNotBlank() && !isImageClip) {
            val targetSpd = effectiveSpeed.coerceIn(0.1f, 8.0f)
            if (abs(exoPlayer.playbackParameters.speed - targetSpd) > 0.03f) {
                exoPlayer.setPlaybackSpeed(targetSpd)
            }
        }
    }

    LaunchedEffect(project.audioClips, abBypassNoiseReduction) {
        val masterAudio = project.audioClips.lastOrNull { !it.title.startsWith("SFX:") } ?: project.audioClips.firstOrNull()
        liveAudioDsp.windFilterEnabled = project.audioClips.any { it.windFilterEnabled }
        liveAudioDsp.noiseGateEnabled = project.audioClips.any { it.noiseGateEnabled }
        liveAudioDsp.noiseReductionStrength = masterAudio?.noiseReductionStrength ?: 0.75f
        liveAudioDsp.abBypassNoiseReduction = abBypassNoiseReduction
        liveAudioDsp.voiceEnhanceEnabled = project.audioClips.any { it.voiceEnhanceEnabled }
        liveAudioDsp.echoRemovalEnabled = project.audioClips.any { it.echoRemovalEnabled }
        liveAudioDsp.vocalMode = masterAudio?.vocalMode ?: VocalIsolationMode.FULL_MIX
    }

    LaunchedEffect(selectedClip?.muted, selectedClip?.volume) {
        // A1 mute controls overlay/audio-track clips, not the V1 source audio.
        val isMuted = selectedClip?.muted == true
        val rawVol = if (isMuted) 0f else (selectedClip?.volume ?: 1f).coerceIn(0f, 1.5f)
        exoPlayer.volume = rawVol.coerceIn(0f, 1f)
        liveAudioDsp.outputMuted = isMuted
        liveAudioDsp.outputGain = rawVol
    }

    val activeAudioClip = project.audioClips.lastOrNull {
        it.mediaUri.isNotBlank() && !it.title.startsWith("SFX:") &&
            playheadMs >= it.startMs && playheadMs < it.startMs + it.durationMs
    }
    val activeAudioUri = activeAudioClip?.mediaUri.orEmpty()

    LaunchedEffect(activeAudioUri) {
        if (activeAudioUri.isNotBlank()) {
            audioPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(activeAudioUri)))
            audioPlayer.prepare()
        } else {
            audioPlayer.stop()
            audioPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(isPlaying, activeAudioUri, project.tracks.a1Muted, activeAudioClip?.volume) {
        if (activeAudioUri.isNotBlank()) {
            audioPlayer.volume = if (project.tracks.a1Muted) 0f else (activeAudioClip?.volume ?: 1f).coerceIn(0f, 1f)
            if (project.tracks.a1Muted) sfxPlayer.volume = 0f
            audioPlayer.playWhenReady = isPlaying
        }
    }

    LaunchedEffect(playheadMs, isPlaying, activeAudioUri) {
        if (activeAudioUri.isNotBlank() && !isPlaying) {
            val offsetMs = (playheadMs - (activeAudioClip?.startMs ?: 0L)).coerceAtLeast(0L)
            if (abs(audioPlayer.currentPosition - offsetMs) > 150L) {
                audioPlayer.seekTo(offsetMs)
            }
        }
        if (isPlaying && !project.tracks.a1Muted) {
            val activeSfx = project.audioClips.firstOrNull {
                it.title.startsWith("SFX:") && it.mediaUri.isNotBlank() && playheadMs in it.startMs..(it.startMs + it.durationMs)
            }
            if (activeSfx != null && lastTriggeredSfxId != activeSfx.id) {
                lastTriggeredSfxId = activeSfx.id
                sfxPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(activeSfx.mediaUri)))
                sfxPlayer.volume = activeSfx.volume.coerceIn(0f, 1f)
                sfxPlayer.prepare()
                sfxPlayer.seekTo((playheadMs - activeSfx.startMs).coerceAtLeast(0L))
                sfxPlayer.playWhenReady = true
            } else if (activeSfx == null) {
                lastTriggeredSfxId = null
            }
        } else {
            lastTriggeredSfxId = null
        }
    }

    val faceSwapSpec = selectedClip?.faceSwap
    LaunchedEffect(faceSwapSpec?.sourceFaceUri) {
        val uri = faceSwapSpec?.sourceFaceUri.orEmpty()
        sourceFaceProfile = if (uri.isNotBlank()) faceSwapEngine.prepareSourceFaceProfile(uri) else null
    }

    val faceScrubBucket = if (isPlaying) 0L else (clipLocalMs / 100L)
    LaunchedEffect(faceSwapSpec?.enabled, faceSwapSpec?.scaleMultiplier, faceSwapSpec?.blendOpacity, sourceFaceProfile, mediaUri, isImageClip, isPlaying, selectedClip?.id, faceScrubBucket) {
        val profile = sourceFaceProfile
        if (faceSwapSpec?.enabled != true || profile == null || mediaUri.isBlank()) {
            faceSwapOverlayBitmap = null
            return@LaunchedEffect
        }
        while (true) {
            val targetMs = (selectedClip?.trimStartMs ?: 0L) + clipLocalMs
            val frame = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                extractPreviewFrame(context, mediaUri, isImageClip, targetMs, 360, 640)
            }
            if (frame != null) {
                val swapped = faceSwapEngine.renderLandmarkFaceOverlay(
                    targetFrame = frame,
                    source = profile,
                    outW = 360,
                    outH = 640,
                    scaleMultiplier = faceSwapSpec.scaleMultiplier,
                    blendOpacity = faceSwapSpec.blendOpacity
                )
                frame.recycle()
                if (swapped != null) {
                    faceSwapOverlayBitmap = swapped
                }
            }
            if (!isPlaying) break
            delay(140L)
        }
    }

    val aiAutoCutEnabled = selectedClip?.chromaKey?.aiAutoCut == true
    val aiBackdropHex = if (project.canvasFillMode == CanvasFillMode.SOLID_COLOR) project.canvasSolidColorHex else 0L

    val scrubBucket = if (isPlaying) 0L else (clipLocalMs / 100L)
    LaunchedEffect(aiAutoCutEnabled, aiBackdropHex, mediaUri, isImageClip, isPlaying, selectedClip?.id, scrubBucket) {
        if (!aiAutoCutEnabled || mediaUri.isBlank()) {
            cutoutBitmap = null
            return@LaunchedEffect
        }
        while (true) {
            val targetMs = (selectedClip?.trimStartMs ?: 0L) + clipLocalMs
            val frame = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                extractPreviewFrame(context, mediaUri, isImageClip, targetMs, 540, 960)
            }
            if (frame != null) {
                val seg = aiAutoCutEngine.segmentForeground(frame, aiBackdropHex)
                frame.recycle()
                if (seg != null) {
                    cutoutBitmap = seg
                }
            }
            if (!isPlaying) break
            delay(85L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(ratio)
                .fillMaxHeight()
                .clip(maskShape)
                .background(
                    when (project.canvasFillMode) {
                        CanvasFillMode.SOLID_COLOR -> Color(project.canvasSolidColorHex)
                        else -> Color.Black
                    }
                )
                .pointerInput(isMotionTrackerMode, isMaskEditMode, isMagicEraserMode, selectedClip?.id) {
                    if (isMagicEraserMode) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            onPaintEraser(
                                (change.position.x / size.width.toFloat()).coerceIn(0.02f, 0.98f),
                                (change.position.y / size.height.toFloat()).coerceIn(0.02f, 0.98f)
                            )
                        }
                    } else if (isMotionTrackerMode) {
                        detectTapGestures { offset ->
                            onTrackerTap(
                                (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f),
                                (offset.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                            )
                        }
                    } else if (isMaskEditMode) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onTransformMask(zoom, pan.x, pan.y, rotation)
                        }
                    } else {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onTransformVideo(zoom, pan.x, pan.y)
                        }
                    }
                }
        ) {
            if (project.tracks.v1Visible && selectedClip != null) {
                if (project.canvasFillMode == CanvasFillMode.BLUR_CANVAS && mediaUri.isNotBlank() && isImageClip) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(project.canvasBlurRadius.dp)
                            .alpha(0.55f)
                    )
                }

                val stageTx = computeClipStageTransform(
                    selectedClip = selectedClip,
                    clipNormT = clipNormT,
                    playheadMs = playheadMs,
                    kf = kf,
                    trackPt = trackPt
                )

                if (mediaUri.isNotBlank() && !isImageClip) {
                    val fxEndCheck = if (selectedClip.videoFxEndMs < 0L) selectedClip.activeDurationMs else selectedClip.videoFxEndMs
                    val isFxInRange = selectedClip.videoFx != VideoFxPreset.NONE &&
                        clipLocalMs in selectedClip.videoFxStartMs..fxEndCheck
                    val glParams = selectedClip.toLiveGlParams(
                        timeSec = (playheadMs / 1000f) * selectedClip.videoFxSpeed.coerceIn(0.25f, 10f),
                        fxActive = isFxInRange
                    ).copy(
                        normTx = stageTx.normTx,
                        normTy = stageTx.normTy,
                        scaleX = stageTx.scaleX,
                        scaleY = stageTx.scaleY,
                        rotZDeg = stageTx.rotZDeg,
                        alpha = stageTx.alpha
                    )
                    KbLiveGlPreviewView(
                        exoPlayer = exoPlayer,
                        params = glParams,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (mediaUri.isNotBlank()) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = selectedClip.title,
                        contentScale = ContentScale.Crop,
                        colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = stageTx.normTx * size.width.coerceAtLeast(360f)
                                translationY = stageTx.normTy * size.height.coerceAtLeast(360f)
                                scaleX = stageTx.scaleX
                                scaleY = stageTx.scaleY
                                rotationX = stageTx.rotX3d
                                rotationY = stageTx.rotY3d
                                cameraDistance = 12f * density
                                rotationZ = stageTx.rotZDeg
                                alpha = stageTx.alpha
                            }
                    )
                    val cg = selectedClip.colorGrade
                    if (cg.vignette > 0.02f || cg.beautyGlow > 0.02f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (cg.beautyGlow > 0.02f) {
                                drawRect(Color.White.copy(alpha = (cg.beautyGlow * 0.22f).coerceIn(0f, 0.35f)))
                            }
                            if (cg.vignette > 0.02f) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = (cg.vignette * 0.78f).coerceIn(0f, 0.85f))),
                                        radius = size.maxDimension * 0.7f
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(SlateVoid, Color(0xFF162238), SlateBase)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedClip.title.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tap + above to import video",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                if (faceSwapSpec?.enabled == true && faceSwapOverlayBitmap != null) {
                    Image(
                        bitmap = faceSwapOverlayBitmap!!.asImageBitmap(),
                        contentDescription = "Landmark Face Swap",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isMaskEditMode && selectedClip.maskType != MaskType.NONE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.5.dp, Color(0xFFFACC15).copy(alpha = 0.75f))
                    ) {
                        Text(
                            "Mask Edit: Drag / Pinch / Rotate",
                            color = Color(0xFFFACC15),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (aiAutoCutEnabled && cutoutBitmap != null) {
                    Image(
                        bitmap = cutoutBitmap!!.asImageBitmap(),
                        contentDescription = "AI Auto-Cut",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (selectedClip.videoFx != VideoFxPreset.NONE) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val fxName = selectedClip.videoFxLabel.ifBlank { selectedClip.videoFx.label }.lowercase()
                        val fxT = (playheadMs / 1000f) * selectedClip.videoFxSpeed.coerceIn(0.25f, 10f)
                        val fxInt = selectedClip.videoFxIntensity.coerceIn(0.2f, 4f)
                        if ("thunder" in fxName || "storm" in fxName || "lightning" in fxName || "rain" in fxName) {
                            val flash = if (((playheadMs / 140L) % 7L) == 0L) (0.38f * fxInt).coerceIn(0.15f, 0.72f) else 0f
                            if (flash > 0f) drawRect(Color(0xFFE0F2FE).copy(alpha = flash))
                            for (rIdx in 0 until 22) {
                                val rx = ((rIdx * 53 + (fxT * 180).toInt()) % 100) / 100f * size.width
                                val ry = ((rIdx * 37 + (fxT * 420).toInt()) % 100) / 100f * size.height
                                drawLine(Color(0xFFBAE6FD).copy(alpha = 0.55f), Offset(rx, ry), Offset(rx - 10f, ry + 34f), strokeWidth = 2.5f)
                            }
                        } else if ("flower" in fxName || "petal" in fxName || "sakura" in fxName || "rose" in fxName || "butterfly" in fxName) {
                            for (pIdx in 0 until 16) {
                                val px = (((pIdx * 67) % 100) / 100f + kotlin.math.sin(fxT * 2f + pIdx) * 0.08f) * size.width
                                val py = (((pIdx * 41 + (fxT * 65).toInt()) % 100) / 100f) * size.height
                                val petalCol = if (pIdx % 2 == 0) Color(0xFFF472B6) else Color(0xFFFDA4AF)
                                drawCircle(petalCol.copy(alpha = 0.85f), radius = 7f + (pIdx % 4) * 2.5f, center = Offset(px, py))
                            }
                        } else if ("star" in fxName || "blink" in fxName || "sparkle" in fxName || "glitter" in fxName || "sequin" in fxName || "magic" in fxName) {
                            for (sIdx in 0 until 18) {
                                val sx = ((sIdx * 59 + 13) % 94 + 3) / 100f * size.width
                                val sy = ((sIdx * 43 + 17) % 92 + 4) / 100f * size.height
                                val pulse = (0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(fxT * 4.5f + sIdx))).coerceIn(0f, 1f)
                                val arm = (8f + (sIdx % 3) * 5f) * pulse
                                val sCol = if (sIdx % 3 == 0) Color(0xFFFDE047) else Color.White
                                drawLine(sCol.copy(alpha = pulse), Offset(sx - arm, sy), Offset(sx + arm, sy), strokeWidth = 2.5f)
                                drawLine(sCol.copy(alpha = pulse), Offset(sx, sy - arm), Offset(sx, sy + arm), strokeWidth = 2.5f)
                            }
                        } else if ("snow" in fxName || "frost" in fxName || "ice" in fxName) {
                            for (fIdx in 0 until 24) {
                                val sx = (((fIdx * 47) % 100) / 100f + kotlin.math.sin(fxT * 1.5f + fIdx) * 0.05f) * size.width
                                val sy = (((fIdx * 29 + (fxT * 45).toInt()) % 100) / 100f) * size.height
                                drawCircle(Color.White.copy(alpha = 0.85f), radius = 3.5f + (fIdx % 3) * 2f, center = Offset(sx, sy))
                            }
                        } else if ("fire" in fxName || "ember" in fxName || "flame" in fxName || "burn" in fxName) {
                            drawRect(Color(0xFFEA580C).copy(alpha = (0.14f * fxInt).coerceIn(0.05f, 0.35f)))
                            for (eIdx in 0 until 18) {
                                val ex = ((eIdx * 61) % 100) / 100f * size.width
                                val ey = size.height - (((eIdx * 31 + (fxT * 90).toInt()) % 100) / 100f) * size.height
                                drawCircle(Color(0xFFFB923C).copy(alpha = 0.88f), radius = 4f + (eIdx % 3) * 2f, center = Offset(ex, ey))
                            }
                        } else if ("heart" in fxName || "love" in fxName || "romantic" in fxName) {
                            drawRect(Color(0xFFEC4899).copy(alpha = (0.12f * fxInt).coerceIn(0.05f, 0.28f)))
                            for (hIdx in 0 until 12) {
                                val hx = ((hIdx * 71) % 90 + 5) / 100f * size.width
                                val hy = size.height - (((hIdx * 37 + (fxT * 55).toInt()) % 100) / 100f) * size.height
                                drawCircle(Color(0xFFF43F5E).copy(alpha = 0.85f), radius = 8f, center = Offset(hx, hy))
                            }
                        } else if ("prism" in fxName || "rainbow" in fxName || "aurora" in fxName) {
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0x33EF4444), Color(0x33EAB308), Color(0x3322C55E), Color(0x333B82F6), Color(0x33A855F7))
                                )
                            )
                        }

                        val cShift = selectedClip.videoFxColorShift
                        if (kotlin.math.abs(cShift) > 0.04f) {
                            val shiftCol = if (cShift > 0f) Color(0xFFFF6B35) else Color(0xFF22D3EE)
                            drawRect(shiftCol.copy(alpha = (kotlin.math.abs(cShift) * 0.24f).coerceIn(0f, 0.30f)))
                        }
                        when (selectedClip.videoFx) {
                            VideoFxPreset.SPLIT_2X -> {
                                drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = 4f)
                                drawRect(Color.Black.copy(alpha = 0.14f), topLeft = Offset(0f, size.height * 0.5f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.5f))
                            }
                            VideoFxPreset.SPLIT_3X -> {
                                drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, size.height * 0.333f), Offset(size.width, size.height * 0.333f), strokeWidth = 4f)
                                drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, size.height * 0.666f), Offset(size.width, size.height * 0.666f), strokeWidth = 4f)
                            }
                            VideoFxPreset.SPLIT_4X -> {
                                drawLine(Color.White.copy(alpha = 0.85f), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 4f)
                                drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 4f)
                            }
                            VideoFxPreset.HALO_BLUR, VideoFxPreset.VIGNETTE_SHOT -> {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f)),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.minDimension * 0.65f
                                    )
                                )
                            }
                            VideoFxPreset.LIGHT_BOKEH, VideoFxPreset.STARLIGHT, VideoFxPreset.GOLD_SEQUINS,
                            VideoFxPreset.GLITTER_BURST, VideoFxPreset.SPARKLE_CRASH -> {
                                val phase = ((playheadMs / 140L) % 6L).toInt()
                                for (k in 0..7) {
                                    val sx = ((k * 137 + phase * 53) % 100) / 100f * size.width
                                    val sy = ((k * 89 + phase * 37) % 100) / 100f * size.height
                                    val c = if (selectedClip.videoFx == VideoFxPreset.GOLD_SEQUINS) WarmAmber else Color.White
                                    drawCircle(c.copy(alpha = 0.55f), radius = 6f + (k % 3) * 4f, center = Offset(sx, sy))
                                }
                            }
                            VideoFxPreset.SOFT_GLOW, VideoFxPreset.DREAM_BLOOM -> {
                                drawRect(Color.White.copy(alpha = 0.14f))
                            }
                            VideoFxPreset.VINTAGE_FILM -> {
                                drawRect(Color(0x28D97706))
                            }
                            VideoFxPreset.BW_GRAIN, VideoFxPreset.BW_SCANNER, VideoFxPreset.SEPIA_NOIR, VideoFxPreset.LOW_EXPOSURE -> {
                                drawRect(Color.Black.copy(alpha = 0.28f))
                            }
                            VideoFxPreset.SPLIT_6X, VideoFxPreset.SPLIT_9X -> {
                                val cols = if (selectedClip.videoFx == VideoFxPreset.SPLIT_6X) 2 else 3
                                for (c in 1 until cols) {
                                    drawLine(Color.White.copy(alpha = 0.85f), Offset(size.width * c / cols, 0f), Offset(size.width * c / cols, size.height), strokeWidth = 3f)
                                }
                                for (r in 1..2) {
                                    drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, size.height * r / 3f), Offset(size.width, size.height * r / 3f), strokeWidth = 3f)
                                }
                            }
                            VideoFxPreset.CIRCLE_OPENING, VideoFxPreset.VERTICAL_OPEN, VideoFxPreset.HORIZONTAL_OPEN, VideoFxPreset.FANCY_CURTAIN -> {
                                val openFrac = (clipNormT * 1.8f).coerceIn(0f, 1f)
                                val barH = size.height * 0.45f * (1f - openFrac)
                                if (barH > 2f) {
                                    drawRect(Color.Black, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, barH))
                                    drawRect(Color.Black, topLeft = Offset(0f, size.height - barH), size = androidx.compose.ui.geometry.Size(size.width, barH))
                                }
                            }
                            VideoFxPreset.WATER_REFLECTION, VideoFxPreset.WATER_RIPPLE, VideoFxPreset.RAIN_PUDDLE, VideoFxPreset.MIRROR_LAKE -> {
                                val waterTop = size.height * 0.62f
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0x5522D3EE), Color(0x880B192C)),
                                        startY = waterTop,
                                        endY = size.height
                                    ),
                                    topLeft = Offset(0f, waterTop),
                                    size = androidx.compose.ui.geometry.Size(size.width, size.height - waterTop)
                                )
                                val waveShift = ((playheadMs / 90L) % 24L).toFloat()
                                var wy = waterTop + 8f
                                while (wy < size.height) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.28f),
                                        start = Offset(waveShift, wy),
                                        end = Offset(size.width - waveShift, wy),
                                        strokeWidth = 2f
                                    )
                                    wy += 16f
                                }
                            }
                            VideoFxPreset.PC_MONITOR, VideoFxPreset.WINDOWS_XP, VideoFxPreset.MAC_WINDOW,
                            VideoFxPreset.TERMINAL_HUD, VideoFxPreset.MEDIA_PLAYER_UI, VideoFxPreset.COUNTDOWN_321 -> {
                                // Desktop window bar
                                drawRect(Color(0xCC111827), topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, 32f))
                                drawCircle(Color(0xFFEF4444), radius = 5f, center = Offset(18f, 16f))
                                drawCircle(Color(0xFFF59E0B), radius = 5f, center = Offset(36f, 16f))
                                drawCircle(Color(0xFF10B981), radius = 5f, center = Offset(54f, 16f))
                                drawRect(ElectricCyan.copy(alpha = 0.55f), style = Stroke(width = 3f))
                                // Player 1-2-3 progress bar at bottom
                                drawRect(Color(0xAA090B10), topLeft = Offset(0f, size.height - 24f), size = androidx.compose.ui.geometry.Size(size.width, 24f))
                                drawRect(ElectricCyan, topLeft = Offset(12f, size.height - 13f), size = androidx.compose.ui.geometry.Size((size.width - 24f) * clipNormT, 4f))
                            }
                            VideoFxPreset.PAGE_FLIP_ALBUM -> {
                                // Book spine shadow & animated page fold edge
                                val foldX = size.width * (1f - (clipNormT * 1.15f).coerceIn(0f, 1f))
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                                        startX = (foldX - 40f).coerceAtLeast(0f),
                                        endX = foldX
                                    ),
                                    topLeft = Offset((foldX - 40f).coerceAtLeast(0f), 0f),
                                    size = androidx.compose.ui.geometry.Size(40f, size.height)
                                )
                                drawLine(Color.White.copy(alpha = 0.7f), Offset(foldX, 0f), Offset(foldX, size.height), strokeWidth = 3f)
                            }
                            VideoFxPreset.CAROUSEL_3D -> {
                                val arc = size.height * 0.06f
                                drawRect(Color.Black, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, arc))
                                drawRect(Color.Black, topLeft = Offset(0f, size.height - arc), size = androidx.compose.ui.geometry.Size(size.width, arc))
                                drawLine(ElectricCyan.copy(alpha = 0.45f), Offset(0f, arc), Offset(size.width, arc), strokeWidth = 2f)
                                drawLine(ElectricCyan.copy(alpha = 0.45f), Offset(0f, size.height - arc), Offset(size.width, size.height - arc), strokeWidth = 2f)
                            }
                            VideoFxPreset.CLOCK_DIAL_4 -> {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val rad = size.minDimension * 0.44f
                                drawCircle(Color.White.copy(alpha = 0.85f), radius = rad, center = Offset(cx, cy), style = Stroke(width = 4f))
                                drawLine(Color.White.copy(alpha = 0.45f), Offset(cx - rad, cy), Offset(cx + rad, cy), strokeWidth = 2f)
                                drawLine(Color.White.copy(alpha = 0.45f), Offset(cx, cy - rad), Offset(cx, cy + rad), strokeWidth = 2f)
                                val minAngle = (clipNormT * 360.0 - 90.0) * Math.PI / 180.0
                                val hrAngle = (clipNormT * 90.0 - 90.0) * Math.PI / 180.0
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(cx, cy),
                                    end = Offset(cx + (rad * 0.65f * kotlin.math.cos(minAngle)).toFloat(), cy + (rad * 0.65f * kotlin.math.sin(minAngle)).toFloat()),
                                    strokeWidth = 7f
                                )
                                drawLine(
                                    color = WarmAmber,
                                    start = Offset(cx, cy),
                                    end = Offset(cx + (rad * 0.42f * kotlin.math.cos(hrAngle)).toFloat(), cy + (rad * 0.42f * kotlin.math.sin(hrAngle)).toFloat()),
                                    strokeWidth = 9f
                                )
                                drawCircle(Color.Black, radius = 9f, center = Offset(cx, cy))
                            }
                            VideoFxPreset.DEPTH_3D_CUBE, VideoFxPreset.PARALLAX_3D, VideoFxPreset.PERSPECTIVE_3D, VideoFxPreset.TUNNEL_3D -> {
                                val inset = size.minDimension * 0.08f
                                drawRect(ElectricCyan.copy(alpha = 0.45f), topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(width = 2f))
                                drawRect(WarmAmber.copy(alpha = 0.35f), topLeft = Offset(inset * 2, inset * 2), size = androidx.compose.ui.geometry.Size(size.width - inset * 4, size.height - inset * 4), style = Stroke(width = 1.5f))
                            }
                            VideoFxPreset.NEON_EDGE -> {
                                drawRect(ElectricCyan.copy(alpha = 0.14f))
                                drawRect(ElectricCyan.copy(alpha = 0.85f), style = Stroke(width = 6f))
                            }
                            VideoFxPreset.VHS_RETRO, VideoFxPreset.OLD_TV, VideoFxPreset.RETRO_FLICKER -> {
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                                    y += 12f
                                }
                            }
                            else -> Unit
                        }
                    }
                }

                if (isMagicEraserMode && selectedClip.magicEraserPoints.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        selectedClip.magicEraserPoints.forEach { pt ->
                            drawCircle(
                                color = ElectricCyan.copy(alpha = 0.25f),
                                radius = size.minDimension * pt.radius,
                                center = Offset(pt.x * size.width, pt.y * size.height)
                            )
                            drawCircle(
                                color = ElectricCyan.copy(alpha = 0.65f),
                                radius = size.minDimension * pt.radius,
                                center = Offset(pt.x * size.width, pt.y * size.height),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                }
                if (selectedClip.watermarkRemover.enabled && !selectedClip.watermarkRemover.cropMode) {
                    val wm = selectedClip.watermarkRemover
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(wm.enabled) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragWatermark(dragAmount.x / size.width.toFloat(), dragAmount.y / size.height.toFloat())
                                }
                            }
                    ) {
                        val bw = size.width * wm.boxWidth
                        val bh = size.height * wm.boxHeight
                        val left = (size.width * wm.posX - bw / 2f).coerceIn(0f, size.width - bw)
                        val top = (size.height * wm.posY - bh / 2f).coerceIn(0f, size.height - bh)
                        drawRoundRect(
                            color = ElectricCyan.copy(alpha = 0.55f),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(bw, bh),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                val cg = selectedClip.colorGrade
                if (isImageClip && (kotlin.math.abs(cg.warmth) > 0.01f || cg.vignette > 0.01f || kotlin.math.abs(cg.brightness) > 0.01f ||
                    cg.saturation < 0.3f || kotlin.math.abs(cg.lift.dx) > 0.02f || kotlin.math.abs(cg.gain.dx) > 0.02f ||
                    cg.beautyGlow > 0.02f || cg.contrast > 1.05f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (cg.warmth > 0.01f) {
                            drawRect(Color(0xFFF59E0B).copy(alpha = (cg.warmth * 0.24f).coerceIn(0f, 0.28f)))
                        } else if (cg.warmth < -0.01f) {
                            drawRect(Color(0xFF06B6D4).copy(alpha = (-cg.warmth * 0.24f).coerceIn(0f, 0.28f)))
                        }
                        if (kotlin.math.abs(cg.lift.dx) > 0.02f || kotlin.math.abs(cg.gain.dx) > 0.02f) {
                            val topCol = if (cg.gain.dx >= 0f) Color(0xFFF59E0B).copy(alpha = 0.14f) else Color(0xFF22D3EE).copy(alpha = 0.14f)
                            val botCol = if (cg.lift.dx <= 0f) Color(0xFF0891B2).copy(alpha = 0.16f) else Color(0xFFEC4899).copy(alpha = 0.16f)
                            drawRect(brush = Brush.verticalGradient(listOf(topCol, botCol)))
                        }
                        if (cg.saturation < 0.3f) {
                            drawRect(Color(0xFF1F2937).copy(alpha = 0.42f))
                        }
                        if (cg.beautyGlow > 0.02f) {
                            drawRect(Color(0xFFFFFBEB).copy(alpha = (cg.beautyGlow * 0.18f).coerceIn(0f, 0.22f)))
                        }
                        if (cg.brightness > 0.01f) {
                            drawRect(Color.White.copy(alpha = (cg.brightness * 0.25f).coerceIn(0f, 0.28f)))
                        } else if (cg.brightness < -0.01f) {
                            drawRect(Color.Black.copy(alpha = (-cg.brightness * 0.35f).coerceIn(0f, 0.38f)))
                        }
                        if (cg.vignette > 0.01f || cg.contrast > 1.05f) {
                            val vigA = maxOf(cg.vignette * 0.75f, (cg.contrast - 1f) * 0.35f).coerceIn(0f, 0.75f)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = vigA)),
                                    radius = size.maxDimension * 0.72f
                                )
                            )
                        }
                    }
                }
                if (selectedClip.maskType == MaskType.ANAMORPHIC_BARS) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.12f)
                            .align(Alignment.TopCenter)
                            .background(Color.Black)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.12f)
                            .align(Alignment.BottomCenter)
                            .background(Color.Black)
                    )
                }
            }

            if (project.tracks.o1Visible) {
                project.pipClips.filter { playheadMs in it.startMs..(it.startMs + it.durationMs) }.forEach { pip ->
                    if (pip.mediaUri.isNotBlank()) {
                        val isPipSelected = pip.id == selectedPipId
                        val isPipVideo = remember(pip.mediaUri) {
                            val low = pip.mediaUri.lowercase()
                            low.endsWith(".mp4") || low.endsWith(".mov") || low.endsWith(".mkv") || low.endsWith(".webm") ||
                                runCatching { context.contentResolver.getType(Uri.parse(pip.mediaUri))?.startsWith("video/") == true }.getOrDefault(false)
                        }
                        val pipBlendAlpha = when (pip.blendMode) {
                            BlendModePreset.SCREEN, BlendModePreset.ADD -> (pip.opacity * 0.85f).coerceIn(0.1f, 1f)
                            BlendModePreset.OVERLAY, BlendModePreset.MULTIPLY -> (pip.opacity * 0.90f).coerceIn(0.1f, 1f)
                            else -> pip.opacity.coerceIn(0.1f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize(pip.scale.coerceIn(0.12f, 1.0f))
                                .align(Alignment.Center)
                                .graphicsLayer {
                                    val extraZoom = (pip.scale / pip.scale.coerceIn(0.12f, 1.0f)).coerceIn(1f, 4f)
                                    scaleX = extraZoom
                                    scaleY = extraZoom
                                    alpha = pipBlendAlpha
                                    rotationZ = pip.rotationDeg
                                    val pipX = trackPt?.x ?: pip.posX
                                    val pipY = trackPt?.y ?: pip.posY
                                    translationX = (pipX - 0.5f) * 360f
                                    translationY = (pipY - 0.5f) * 640f
                                }
                                .pointerInput(pip.id) {
                                    detectTapGestures { onSelectPip(pip.id) }
                                }
                                .pointerInput(pip.id) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        onSelectPip(pip.id)
                                        onTransformPip(pip.id, zoom, pan.x, pan.y, rotation)
                                    }
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isPipSelected) Modifier.border(2.dp, ElectricCyan, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                        ) {
                            if (isPipVideo) {
                                val pipPlayer = remember(pip.id, pip.mediaUri) {
                                    ExoPlayer.Builder(context, renderersFactory).build().apply {
                                        setMediaItem(MediaItem.fromUri(Uri.parse(pip.mediaUri)))
                                        prepare()
                                        volume = if (project.tracks.a1Muted) 0f else pip.opacity.coerceIn(0f, 1f)
                                    }
                                }
                                DisposableEffect(pipPlayer) {
                                    onDispose { pipPlayer.release() }
                                }
                                LaunchedEffect(isPlaying, playheadMs, pip.startMs) {
                                    val pipLocal = (playheadMs - pip.startMs).coerceAtLeast(0L)
                                    if (!isPlaying && abs(pipPlayer.currentPosition - pipLocal) > 120L) {
                                        pipPlayer.seekTo(pipLocal)
                                    } else if (isPlaying && abs(pipPlayer.currentPosition - pipLocal) > 800L) {
                                        pipPlayer.seekTo(pipLocal)
                                    }
                                    pipPlayer.playWhenReady = isPlaying
                                }
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            player = pipPlayer
                                        }
                                    },
                                    update = { it.player = pipPlayer },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = pip.mediaUri,
                                    contentDescription = pip.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (isPipSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xDDDC2626))
                                        .clickable { onDeletePip(pip.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (pip.opticalFxPreset?.startsWith("STICKER:") == true) {
                        val stickerLabel = pip.opticalFxPreset.removePrefix("STICKER:")
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stickerLabel,
                                fontSize = (22f * pip.scale.coerceIn(0.25f, 4.0f)).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(
                                        x = (((trackPt?.x ?: pip.posX) - 0.5f) * 180).dp,
                                        y = (((trackPt?.y ?: pip.posY) - 0.5f) * 260).dp
                                    )
                                    .graphicsLayer {
                                        rotationZ = pip.rotationDeg
                                    }
                                    .pointerInput(pip.id) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            onSelectPip(pip.id)
                                            onTransformPip(pip.id, zoom, pan.x, pan.y, rotation)
                                        }
                                    }
                                    .background(Color(0xCC111827), RoundedCornerShape(14.dp))
                                    .border(1.5.dp, WarmAmber, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        val composeBlend = when (pip.blendMode) {
                            BlendModePreset.NORMAL -> BlendMode.SrcOver
                            BlendModePreset.SCREEN -> BlendMode.Screen
                            BlendModePreset.MULTIPLY -> BlendMode.Multiply
                            BlendModePreset.ADD -> BlendMode.Plus
                            BlendModePreset.OVERLAY -> BlendMode.Overlay
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        ) {
                            val center = Offset(size.width * pip.posX, size.height * pip.posY)
                            val radius = size.minDimension * 0.28f * pip.scale
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(ElectricCyan.copy(alpha = pip.opacity), WarmAmber.copy(alpha = 0.25f), Color.Transparent),
                                    center = center,
                                    radius = radius
                                ),
                                center = center,
                                radius = radius,
                                blendMode = composeBlend
                            )
                        }
                    }
                }
            }

            if (project.tracks.t1Visible) {
                project.captionClips.filter { playheadMs in it.startMs..(it.startMs + it.durationMs) }.forEach { cap ->
                    val capProgress = ((playheadMs - cap.startMs).toFloat() / cap.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    val animScale = when (cap.animation3D) {
                        MotionAnimation3D.POP_3D -> if (capProgress < 0.2f) 0.6f + capProgress * 2.2f else 1f
                        MotionAnimation3D.FLOAT_NEON -> 1f + 0.04f * sin(capProgress * 12f)
                        else -> 1f
                    }
                    val rotY = if (cap.animation3D == MotionAnimation3D.FLIP_3D && capProgress < 0.3f) {
                        (1f - capProgress / 0.3f) * 75f
                    } else 0f

                    val anchorX = trackPt?.x ?: cap.posX
                    val anchorY = trackPt?.y ?: cap.posY

                    val rawTxt = if (cap.animation3D == MotionAnimation3D.TYPEWRITER) {
                        cap.text.take((cap.text.length * (capProgress * 2f).coerceAtMost(1f)).toInt().coerceAtLeast(1))
                    } else cap.text
                    val displayTxt = if (cap.stylePreset == CaptionStylePreset.VIRAL_HORMOZI || cap.stylePreset == CaptionStylePreset.MINIMAL_TAG) {
                        rawTxt.uppercase()
                    } else rawTxt

                    val bgModifier = when (cap.stylePreset) {
                        CaptionStylePreset.VIRAL_HORMOZI -> Modifier
                            .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color(cap.accentHex), RoundedCornerShape(8.dp))
                        CaptionStylePreset.NEON_CYBER -> Modifier
                            .background(Color(0xCC060913), RoundedCornerShape(10.dp))
                            .border(1.5.dp, ElectricCyan, RoundedCornerShape(10.dp))
                        CaptionStylePreset.DARK_PILL -> Modifier
                            .background(Color(0xDD090B10), RoundedCornerShape(22.dp))
                        CaptionStylePreset.GRADIENT_POP -> Modifier
                            .background(
                                Brush.horizontalGradient(listOf(VioletClip, Color(0xFFEC4899), WarmAmber)),
                                RoundedCornerShape(12.dp)
                            )
                        CaptionStylePreset.GOLD_LUXURY -> Modifier
                            .background(Color(0xDD0B0E14), RoundedCornerShape(10.dp))
                            .border(1.5.dp, WarmAmber, RoundedCornerShape(10.dp))
                        CaptionStylePreset.OUTLINE_IMPACT -> Modifier
                        CaptionStylePreset.CINEMA_SUB -> Modifier
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                        CaptionStylePreset.MINIMAL_TAG -> Modifier
                            .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(6.dp))
                    }

                    val textColor = when (cap.stylePreset) {
                        CaptionStylePreset.MINIMAL_TAG -> Color(0xFF090B10)
                        CaptionStylePreset.VIRAL_HORMOZI, CaptionStylePreset.GOLD_LUXURY -> Color(cap.accentHex)
                        else -> Color(cap.colorHex)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = displayTxt,
                            color = textColor,
                            fontSize = (cap.fontSizeSp * 0.72f).coerceIn(11f, 26f).sp,
                            fontWeight = if (cap.isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontFamily = when (cap.fontStyle) {
                                CaptionFontStyle.SANS_BOLD, CaptionFontStyle.ROUNDED_POP -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                CaptionFontStyle.SERIF_EDITORIAL -> androidx.compose.ui.text.font.FontFamily.Serif
                                CaptionFontStyle.MONO_CODE -> androidx.compose.ui.text.font.FontFamily.Monospace
                            },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = if (cap.stylePreset == CaptionStylePreset.NEON_CYBER) ElectricCyan else Color.Black,
                                    offset = Offset(0f, 3f),
                                    blurRadius = if (cap.stylePreset == CaptionStylePreset.NEON_CYBER) 12f else 6f
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(
                                    x = ((anchorX - 0.5f) * 140).dp,
                                    y = ((anchorY - 0.8f) * 260).dp
                                )
                                .graphicsLayer {
                                    scaleX = animScale
                                    scaleY = animScale
                                    rotationY = rotY
                                    rotationZ = cap.rotationDeg
                                    cameraDistance = 12f * density
                                }
                                .pointerInput(cap.id) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        onTransformCaption(cap.id, zoom, pan.x, pan.y, rotation)
                                    }
                                }
                                .then(bgModifier)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (trackPt != null) {
                val is3d = selectedClip?.motionTrackMode == "3D"
                val retX = if (is3d) 0.5f else trackPt.x
                val retY = if (is3d) 0.5f else trackPt.y
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width * retX
                        val cy = size.height * retY
                        val boxR = 32f * trackPt.scale
                        drawCircle(
                            color = ElectricCyan,
                            radius = boxR,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3f)
                        )
                        drawRect(
                            color = WarmAmber.copy(alpha = 0.85f),
                            topLeft = Offset(cx - boxR * 1.15f, cy - boxR * 1.15f),
                            size = androidx.compose.ui.geometry.Size(boxR * 2.3f, boxR * 2.3f),
                            style = Stroke(width = 2f)
                        )
                        drawLine(ElectricCyan, Offset(cx - boxR * 1.4f, cy), Offset(cx + boxR * 1.4f, cy), strokeWidth = 2f)
                        drawLine(ElectricCyan, Offset(cx, cy - boxR * 1.4f), Offset(cx, cy + boxR * 1.4f), strokeWidth = 2f)
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (is3d) "3D CAMERA FOLLOW" else "2D TRACK LOCKED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (selectedClip != null && (selectedClip.zoomScale != 1f || selectedClip.panX != 0f || selectedClip.panY != 0f || selectedClip.rotationDeg != 0f)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .clickable { onResetTransform() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Reset ${"%.1f".format(selectedClip.zoomScale)}x",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }

            if (isMaskEditMode && selectedClip != null && selectedClip.maskType != MaskType.NONE) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mCx = size.width * (0.5f + selectedClip.maskOffsetX)
                    val mCy = size.height * (0.5f + selectedClip.maskOffsetY)
                    val mRad = size.minDimension * 0.42f * selectedClip.maskScale.coerceIn(0.25f, 1.4f)
                    val radAngle = Math.toRadians(selectedClip.maskRotationDeg.toDouble())
                    val cosA = kotlin.math.cos(radAngle).toFloat()
                    val sinA = kotlin.math.sin(radAngle).toFloat()
                    // CapCut-style golden yellow outline
                    drawCircle(
                        color = WarmAmber,
                        radius = mRad,
                        center = Offset(mCx, mCy),
                        style = Stroke(width = 3f)
                    )
                    // Center move ring
                    drawCircle(color = WarmAmber, radius = 12f, center = Offset(mCx, mCy), style = Stroke(width = 3f))
                    // Edge handle dots (Right = resize, Top = rotate)
                    val rightPt = Offset(mCx + mRad * cosA, mCy + mRad * sinA)
                    val topPt = Offset(mCx + mRad * sinA, mCy - mRad * cosA)
                    val leftPt = Offset(mCx - mRad * cosA, mCy - mRad * sinA)
                    val botPt = Offset(mCx - mRad * sinA, mCy + mRad * cosA)
                    listOf(rightPt, topPt, leftPt, botPt).forEach { pt ->
                        drawCircle(color = Color.White, radius = 11f, center = pt)
                        drawCircle(color = WarmAmber, radius = 11f, center = pt, style = Stroke(width = 2.5f))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable { onToggleFullScreen() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isFullScreen) "✕" else "⛶",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            if (selectedClip?.videoFx == VideoFxPreset.INFINITE_TUNNEL && project.videoClips.isNotEmpty()) {
                val clips = project.videoClips
                val phase = (playheadMs / 45L).toFloat()
                // Background 2x3 mini collage + concentric infinite 3D tunnel rings + bottom wave reflection
                Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    for (r in 0..2) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0..1) {
                                val u = clips[(r * 2 + c) % clips.size].mediaUri
                                AsyncImage(
                                    model = u,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.44f
                    val maxR = size.maxDimension * 0.65f
                    for (ring in 0..6) {
                        val frac = ((ring / 7f) + (phase % 30f) / 210f) % 1f
                        val rad = maxR * frac
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.55f * (1f - frac * 0.5f)),
                            radius = rad,
                            center = Offset(cx, cy),
                            style = Stroke(width = 14f)
                        )
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.25f),
                            radius = rad,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5f)
                        )
                    }
                    drawCircle(Color.Black, radius = size.minDimension * 0.14f, center = Offset(cx, cy))
                    // Bottom wave reflection
                    val wTop = size.height * 0.72f
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x880B192C)), startY = wTop, endY = size.height),
                        topLeft = Offset(0f, wTop),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height - wTop)
                    )
                }
            }

            if (selectedClip?.videoFx == VideoFxPreset.DIARY_FRAME_2 && selectedClip.mediaUri.isNotBlank()) {
                val tilt = kotlin.math.sin(clipNormT * 3.14f) * 5f - 2.5f
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = selectedClip.mediaUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(14.dp)
                            .alpha(0.55f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .aspectRatio(1.35f)
                            .graphicsLayer { rotationZ = tilt }
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        AsyncImage(
                            model = selectedClip.mediaUri,
                            contentDescription = "Diary Frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (selectedClip?.videoFx == VideoFxPreset.CAROUSEL_3D && project.videoClips.isNotEmpty()) {
                val clips = project.videoClips
                val totalProg = (playheadMs.toFloat() / project.totalDurationMs.coerceAtLeast(1000L).toFloat()).coerceIn(0f, 1f)
                val slideX = totalProg * (clips.size.coerceAtLeast(1) * 260f)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .graphicsLayer {
                                translationX = 120f - slideX
                                rotationY = -12f
                                cameraDistance = 14f * density
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        clips.forEach { cc ->
                            AsyncImage(
                                model = cc.mediaUri,
                                contentDescription = cc.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(240.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    // Bottom Water Wave Reflection
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val wTop = size.height * 0.68f
                        drawRect(
                            brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA0A192F), Color.Black), startY = wTop, endY = size.height),
                            topLeft = Offset(0f, wTop),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height - wTop)
                        )
                        val waveShift = ((playheadMs / 80L) % 24L).toFloat()
                        var wy = wTop + 10f
                        while (wy < size.height) {
                            drawLine(Color(0x5522D3EE), Offset(waveShift, wy), Offset(size.width - waveShift, wy), strokeWidth = 2f)
                            wy += 14f
                        }
                    }
                }
            }

            if (selectedClip?.videoFx == VideoFxPreset.SPLIT_FILMSTRIP && project.videoClips.isNotEmpty()) {
                val heroUri = project.videoClips.first().mediaUri
                val totalProg = (playheadMs.toFloat() / project.totalDurationMs.coerceAtLeast(1000L).toFloat()).coerceIn(0f, 1f)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left half: Main portrait with soft glow
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = heroUri,
                            contentDescription = "Main Portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Right half: Vertical scrolling filmstrip of all clips
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black)
                            .clip(RoundedCornerShape(0.dp))
                    ) {
                        val stripClips = project.videoClips
                        val slideOffset = totalProg * (stripClips.size.coerceAtLeast(1) * 160f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationY = -slideOffset }
                        ) {
                            stripClips.forEach { sc ->
                                AsyncImage(
                                    model = sc.mediaUri,
                                    contentDescription = sc.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                // Floating glowing pink/purple heart particles across the split screen
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val phase = ((playheadMs / 120L) % 20L).toInt()
                    for (h in 0..7) {
                        val hx = ((h * 113 + phase * 19) % 100) / 100f * size.width
                        val hy = ((100 - ((h * 67 + phase * 23) % 100)) / 100f) * size.height
                        drawCircle(Color(0x88EC4899), radius = 14f + (h % 3) * 5f, center = Offset(hx, hy))
                        drawCircle(Color(0xAAFFFFFF), radius = 5f, center = Offset(hx, hy))
                    }
                }
            }

            if (project.safeZoneVisible) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val insetX = size.width * 0.08f
                    val insetY = size.height * 0.10f
                    drawRect(
                        color = WarmAmber.copy(alpha = 0.65f),
                        topLeft = Offset(insetX, insetY),
                        size = androidx.compose.ui.geometry.Size(size.width - insetX * 2, size.height - insetY * 2),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }
    }
}

private data class ClipStageTransform(
    val normTx: Float,
    val normTy: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotZDeg: Float,
    val rotX3d: Float,
    val rotY3d: Float,
    val alpha: Float
)

private fun computeClipStageTransform(
    selectedClip: VideoClip,
    clipNormT: Float,
    playheadMs: Long,
    kf: KeyframePoint?,
    trackPt: KeyframePoint?
): ClipStageTransform {
    val clipDurMs = selectedClip.activeDurationMs.coerceAtLeast(300L)
    val clipPosMs = (clipNormT * clipDurMs).toLong()
    val fxEnd = if (selectedClip.videoFxEndMs < 0L) clipDurMs else selectedClip.videoFxEndMs.coerceIn(0L, clipDurMs)
    val fxActive = selectedClip.videoFx != VideoFxPreset.NONE && clipPosMs in selectedClip.videoFxStartMs..fxEnd
    val fxI = if (fxActive) selectedClip.videoFxIntensity.coerceIn(0.1f, 10f) else 0f
    val fxT = clipNormT * selectedClip.videoFxSpeed.coerceIn(0.25f, 10f)

    val shakeX = if (!fxActive) 0f else fxI * when {
        selectedClip.videoFx in listOf(VideoFxPreset.CAM_SHAKE, VideoFxPreset.GLITCH_RGB, VideoFxPreset.PHONE_SWAG, VideoFxPreset.TEAR_APART) -> kotlin.math.sin(fxT * 48f) * 0.025f
        selectedClip.videoFx in listOf(VideoFxPreset.EARTHQUAKE, VideoFxPreset.SINGLE_SHOCK, VideoFxPreset.MANGA_SHOCK) -> kotlin.math.sin(fxT * 72f) * 0.042f
        selectedClip.videoFx in listOf(VideoFxPreset.CAM_SWING, VideoFxPreset.REBOUND_SWING, VideoFxPreset.PENDULUM_SWING) -> kotlin.math.sin(fxT * 12f) * 0.046f
        selectedClip.videoFx == VideoFxPreset.SLIGHT_SHAKE -> kotlin.math.sin(fxT * 30f) * 0.014f
        else -> 0f
    }
    val shakeY = if (!fxActive) 0f else fxI * when {
        selectedClip.videoFx == VideoFxPreset.CAM_SHAKE -> kotlin.math.cos(clipNormT * 36f) * 0.018f
        selectedClip.videoFx == VideoFxPreset.EARTHQUAKE -> kotlin.math.cos(clipNormT * 64f) * 0.035f
        selectedClip.videoFx in listOf(VideoFxPreset.BOUNCE_FX, VideoFxPreset.BOUNCING_SNOW, VideoFxPreset.FLASH_BOUNCE) -> -kotlin.math.abs(kotlin.math.sin(clipNormT * 16f)) * 0.04f
        else -> 0f
    }
    val fxScale = if (!fxActive) 1f else when {
        selectedClip.videoFx in listOf(VideoFxPreset.CAM_SHAKE, VideoFxPreset.EARTHQUAKE, VideoFxPreset.SINGLE_SHOCK) -> 1.08f
        selectedClip.videoFx in listOf(VideoFxPreset.ZOOM_PULSE, VideoFxPreset.QUANTUM_PULSE, VideoFxPreset.RADIANT_PULSE) -> 1f + 0.09f * kotlin.math.abs(sin(clipNormT * 18f))
        selectedClip.videoFx == VideoFxPreset.HEARTBEAT -> 1f + 0.11f * kotlin.math.abs(sin(clipNormT * 10f)) * kotlin.math.abs(sin(clipNormT * 20f))
        selectedClip.videoFx in listOf(VideoFxPreset.ZOOM_LENS, VideoFxPreset.ZOOM_FLASH, VideoFxPreset.TILT_ZOOM_IN, VideoFxPreset.ZOOM_CURSOR) -> 1f + clipNormT * 0.25f
        selectedClip.videoFx in listOf(VideoFxPreset.MINI_ZOOM, VideoFxPreset.SOFT_ZOOM) -> 1f + clipNormT * 0.12f
        selectedClip.videoFx == VideoFxPreset.ZOOM_OUT_FX -> 1.25f - clipNormT * 0.25f
        selectedClip.videoFx in listOf(VideoFxPreset.RING_PULSE, VideoFxPreset.DOODLE_PULSE, VideoFxPreset.METEOR_CURVE) -> 1f + 0.07f * kotlin.math.abs(sin(clipNormT * 24f))
        else -> 1f
    }

    val transT = if (selectedClip.transitionOut != TransitionType.NONE && clipNormT > 0.85f) {
        ((clipNormT - 0.85f) / 0.15f).coerceIn(0f, 1f)
    } else 0f
    val transAlpha = when (selectedClip.transitionOut) {
        TransitionType.CROSS_DISSOLVE -> 1f - transT * 0.85f
        TransitionType.FLASH_CUT -> if (transT > 0.4f) 0.35f else 1f
        else -> 1f
    }
    val transPanX = if (selectedClip.transitionOut == TransitionType.WHIP_PAN) -transT * 0.45f else 0f
    val wmCropZoom = if (selectedClip.watermarkRemover.enabled && selectedClip.watermarkRemover.cropMode) 1.14f else 1f
    val transZoom = (if (selectedClip.transitionOut == TransitionType.ZOOM_THROUGH) 1f + transT * 0.65f else 1f) * wmCropZoom

    val fadeInAlpha = if (selectedClip.fadeInMs > 0L && clipPosMs < selectedClip.fadeInMs) {
        (clipPosMs.toFloat() / selectedClip.fadeInMs.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val fadeOutAlpha = if (selectedClip.fadeOutMs > 0L && (clipDurMs - clipPosMs) < selectedClip.fadeOutMs) {
        ((clipDurMs - clipPosMs).toFloat() / selectedClip.fadeOutMs.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val fxAlpha = (when (selectedClip.videoFx) {
        VideoFxPreset.FLASH_STROBE, VideoFxPreset.CAMERA_STROBE -> if ((playheadMs / 120L) % 2L == 0L) 0.65f else 1f
        VideoFxPreset.ACTION_FLASH, VideoFxPreset.DECOLOR_FLASH -> if ((playheadMs / 90L) % 3L == 0L) 0.45f else 1f
        VideoFxPreset.WHITE_FLASH, VideoFxPreset.BLACK_FLASH_2, VideoFxPreset.FLICKER_BLACK -> if ((playheadMs / 160L) % 3L == 0L) 0.55f else 1f
        VideoFxPreset.RETRO_FLICKER -> if ((playheadMs / 110L) % 4L == 0L) 0.72f else 1f
        VideoFxPreset.FADE_IN_FX, VideoFxPreset.OPENING_ARC -> (clipNormT * 2.5f).coerceIn(0f, 1f)
        VideoFxPreset.FADE_OUT_FX, VideoFxPreset.THE_END_FX, VideoFxPreset.DARK_FADE -> ((1f - clipNormT) * 2.5f).coerceIn(0f, 1f)
        else -> 1f
    }) * transAlpha * fadeInAlpha * fadeOutAlpha

    val hasKfs = selectedClip.keyframes.isNotEmpty()
    val basePanX = if (hasKfs) ((kf?.x ?: 0.5f) - 0.5f) else (selectedClip.panX / 560f)
    val basePanY = if (hasKfs) ((kf?.y ?: 0.5f) - 0.5f) else (selectedClip.panY / 560f)
    val baseScale = if (hasKfs) (kf?.scale ?: 1f) else selectedClip.zoomScale

    val is3dFollow = trackPt != null
    val followPanX = if (is3dFollow) (0.5f - trackPt!!.x) * 0.35f else 0f
    val followPanY = if (is3dFollow) (0.5f - trackPt!!.y) * 0.38f else 0f
    val followScale = if (is3dFollow) 1.14f * trackPt!!.scale else 1f

    val animDur = selectedClip.animDurationMs.coerceIn(200L, clipDurMs)
    val inP = if (selectedClip.animIn != ClipAnimPreset.NONE && clipPosMs < animDur) {
        (clipPosMs.toFloat() / animDur.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val outP = if (selectedClip.animOut != ClipAnimPreset.NONE && (clipDurMs - clipPosMs) < animDur) {
        ((clipDurMs - clipPosMs).toFloat() / animDur.toFloat()).coerceIn(0f, 1f)
    } else 1f

    var animTx = 0f; var animTy = 0f; var animSc = 1f; var animRotZ = 0f; var animAl = 1f
    if (inP < 1f) {
        when (selectedClip.animIn) {
            ClipAnimPreset.IN_FADE, ClipAnimPreset.IN_GLOW_FADE, ClipAnimPreset.IN_BLUR -> animAl *= inP
            ClipAnimPreset.IN_ZOOM_1, ClipAnimPreset.IN_ZOOM_IN, ClipAnimPreset.IN_MINI_ZOOM -> { animSc *= (0.45f + 0.55f * inP); animAl *= inP }
            ClipAnimPreset.IN_SLIDE_RIGHT -> animTx += -(1f - inP) * 0.55f
            ClipAnimPreset.IN_SLIDE_LEFT -> animTx += (1f - inP) * 0.55f
            ClipAnimPreset.IN_SLIDE_UP -> animTy += (1f - inP) * 0.55f
            ClipAnimPreset.IN_SLIDE_DOWN -> animTy += -(1f - inP) * 0.55f
            ClipAnimPreset.IN_SPIN, ClipAnimPreset.IN_FLIP, ClipAnimPreset.IN_CUBE_ROT -> { animRotZ += (1f - inP) * 180f; animSc *= (0.6f + 0.4f * inP) }
            ClipAnimPreset.IN_SHAKE_1, ClipAnimPreset.IN_SHAKE_2, ClipAnimPreset.IN_ROCK_VERT, ClipAnimPreset.IN_SUDDEN_IMPACT -> animTx += kotlin.math.sin(inP * 36f) * (1f - inP) * 0.05f
            ClipAnimPreset.IN_BEAT_BOUNCE, ClipAnimPreset.IN_BASS_PULSE -> animSc *= (1f + kotlin.math.sin(inP * 14f) * (1f - inP) * 0.25f)
            else -> { animSc *= (0.75f + 0.25f * inP); animAl *= inP }
        }
    }
    if (outP < 1f) {
        when (selectedClip.animOut) {
            ClipAnimPreset.OUT_FADE, ClipAnimPreset.OUT_BLUR, ClipAnimPreset.OUT_STORM_FLASH -> animAl *= outP
            ClipAnimPreset.OUT_ZOOM, ClipAnimPreset.OUT_ZOOM_BACK, ClipAnimPreset.OUT_SHRINK -> { animSc *= (0.45f + 0.55f * outP); animAl *= outP }
            ClipAnimPreset.OUT_SLIDE_LEFT -> animTx += -(1f - outP) * 0.55f
            ClipAnimPreset.OUT_SLIDE_RIGHT -> animTx += (1f - outP) * 0.55f
            ClipAnimPreset.OUT_SLIDE_UP -> animTy += -(1f - outP) * 0.55f
            ClipAnimPreset.OUT_SLIDE_DOWN -> animTy += (1f - outP) * 0.55f
            ClipAnimPreset.OUT_SPIN, ClipAnimPreset.OUT_FLIP, ClipAnimPreset.OUT_CUBE, ClipAnimPreset.OUT_SKEW -> { animRotZ += (1f - outP) * 180f; animSc *= outP.coerceAtLeast(0.25f) }
            ClipAnimPreset.OUT_SHAKE, ClipAnimPreset.OUT_JITTER -> animTx += kotlin.math.sin(outP * 36f) * (1f - outP) * 0.05f
            else -> { animSc *= (0.6f + 0.4f * outP); animAl *= outP }
        }
    }
    val animI = selectedClip.animIntensity.coerceIn(0.2f, 10f)
    val comboT = clipNormT * selectedClip.animSpeed.coerceIn(0.25f, 10f)
    if (selectedClip.animCombo != ClipAnimPreset.NONE) {
        when (selectedClip.animCombo) {
            ClipAnimPreset.COMBO_PENDULUM, ClipAnimPreset.COMBO_SWING_BOT, ClipAnimPreset.COMBO_SWING_R -> animRotZ += kotlin.math.sin(comboT * 12f) * 8f * animI
            ClipAnimPreset.COMBO_DISTORT_SPIN, ClipAnimPreset.COMBO_ZOOM_SPIN, ClipAnimPreset.COMBO_QUICK_SPIN, ClipAnimPreset.COMBO_VORTEX, ClipAnimPreset.COMBO_WHIRL, ClipAnimPreset.COMBO_ORBIT_3D, ClipAnimPreset.COMBO_SPIN_BLUR -> { animRotZ += comboT * 360f * animI; animSc *= 1.08f }
            ClipAnimPreset.COMBO_BOUNCE, ClipAnimPreset.COMBO_ELASTIC, ClipAnimPreset.COMBO_STRETCH_FALL -> animTy += -kotlin.math.abs(kotlin.math.sin(clipNormT * 14f)) * 0.045f * animI
            ClipAnimPreset.COMBO_WOBBLE, ClipAnimPreset.COMBO_BASS_SHAKE, ClipAnimPreset.COMBO_SONIC_WAVE -> animTx += kotlin.math.sin(clipNormT * 28f) * 0.03f * animI
            ClipAnimPreset.COMBO_3D_ZOOM, ClipAnimPreset.COMBO_HEARTBEAT, ClipAnimPreset.COMBO_FLOATING -> animSc *= (1f + 0.12f * animI * kotlin.math.abs(kotlin.math.sin(clipNormT * 10f)))
            else -> animSc *= (1f + 0.08f * animI * kotlin.math.sin(clipNormT * 12f))
        }
    }

    val rotX3d = if (fxActive) when (selectedClip.videoFx) {
        VideoFxPreset.PERSPECTIVE_3D -> 18f * fxI
        VideoFxPreset.DEPTH_3D_CUBE -> kotlin.math.sin(fxT * 6f) * 22f * fxI
        else -> 0f
    } else 0f
    val rotY3d = if (fxActive) when (selectedClip.videoFx) {
        VideoFxPreset.PARALLAX_3D -> kotlin.math.cos(fxT * 6f) * 20f * fxI
        VideoFxPreset.DEPTH_3D_CUBE -> kotlin.math.cos(fxT * 6f) * 24f * fxI
        VideoFxPreset.TUNNEL_3D -> kotlin.math.sin(fxT * 10f) * 14f * fxI
        VideoFxPreset.PAGE_FLIP_ALBUM -> -kotlin.math.abs(kotlin.math.sin(clipNormT * Math.PI.toFloat())) * 38f * fxI
        VideoFxPreset.CAROUSEL_3D -> kotlin.math.sin(clipNormT * Math.PI.toFloat() * 2f) * 28f * fxI
        else -> 0f
    } else 0f

    val finalScaleY = baseScale * followScale * fxScale * transZoom * animSc
    val finalScaleX = finalScaleY * (if (selectedClip.flipHorizontal) -1f else 1f)
    return ClipStageTransform(
        normTx = selectedClip.reframeOffsetX * 0.22f + basePanX + followPanX + shakeX + transPanX + animTx,
        normTy = basePanY + followPanY + shakeY + animTy,
        scaleX = finalScaleX,
        scaleY = finalScaleY,
        rotZDeg = (kf?.rotationDeg ?: 0f) + selectedClip.rotationDeg + animRotZ,
        rotX3d = rotX3d,
        rotY3d = rotY3d,
        alpha = ((kf?.opacity ?: 1f) * fxAlpha * animAl).coerceIn(0f, 1f)
    )
}

private fun extractPreviewFrame(
    context: Context,
    mediaUri: String,
    isImage: Boolean,
    timeMs: Long,
    reqW: Int,
    reqH: Int
): Bitmap? {
    if (mediaUri.isBlank()) return null
    val uri = Uri.parse(mediaUri)
    if (isImage) {
        return runCatching {
            val optsBounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (uri.scheme == "file" && uri.path != null) {
                android.graphics.BitmapFactory.decodeFile(uri.path, optsBounds)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, optsBounds)
                }
            }
            var sample = 1
            while (optsBounds.outWidth / sample > reqW * 2 && optsBounds.outHeight / sample > reqH * 2) {
                sample *= 2
            }
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = if (uri.scheme == "file" && uri.path != null) {
                android.graphics.BitmapFactory.decodeFile(uri.path, opts)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it, null, opts)
                }
            } ?: return null
            Bitmap.createScaledBitmap(raw, reqW, reqH, true).also {
                if (it !== raw) raw.recycle()
            }
        }.getOrNull()
    }
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        if (uri.scheme.isNullOrEmpty()) retriever.setDataSource(mediaUri)
        else retriever.setDataSource(context, uri)
        val us = (timeMs.coerceAtLeast(0L)) * 1000L
        val bmp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(us, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, reqW, reqH)
        } else {
            retriever.getFrameAtTime(us, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                Bitmap.createScaledBitmap(it, reqW, reqH, true).also { s -> if (s !== it) it.recycle() }
            }
        }
        bmp
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}
