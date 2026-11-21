package com.aistudio.kbeditor.r7v4qx.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RadialGradient
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.LinearGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import com.aistudio.kbeditor.r7v4qx.model.CaptionFontStyle
import com.aistudio.kbeditor.r7v4qx.model.CaptionStylePreset
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.aistudio.kbeditor.r7v4qx.audio.StudioAudioDspProcessor
import com.aistudio.kbeditor.r7v4qx.model.MaskType
import com.aistudio.kbeditor.r7v4qx.vfx.ChromaKeyGlEffect
import com.aistudio.kbeditor.r7v4qx.vfx.CinematicGradeGlEffect
import com.aistudio.kbeditor.r7v4qx.model.VideoFxPreset
import com.aistudio.kbeditor.r7v4qx.vfx.MaskGlEffect
import com.aistudio.kbeditor.r7v4qx.vfx.VideoFxGlEffect
import com.aistudio.kbeditor.r7v4qx.vision.AiAutoCutEngine
import com.aistudio.kbeditor.r7v4qx.vision.FaceSwapEngine
import com.aistudio.kbeditor.r7v4qx.model.ColorGradeSpec
import com.aistudio.kbeditor.r7v4qx.model.ExportDynamicRange
import com.aistudio.kbeditor.r7v4qx.model.ExportResolution
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@UnstableApi
class VideoExportEngine(private val context: Context) {

    suspend fun exportProject(
        project: KbEditorProject,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.Main) {
        val validClips = project.videoClips.filter { it.mediaUri.isNotBlank() }
        if (validClips.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Import at least one video clip before exporting."))
        }

        val outFile = File(context.cacheDir, "kb_export_${System.currentTimeMillis()}.mp4")
        val targetWidth = computeTargetWidth(project)
        val targetHeight = computeTargetHeight(project)

        var runningOffsetMs = 0L
        val editedVideoItems = validClips.map { clip ->
            val clipStartInProjectMs = runningOffsetMs
            runningOffsetMs += clip.activeDurationMs
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.trimStartMs)
                .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 300L))
                .build()
            val isImg = clip.isAiGeneratedBroll || clip.mediaUri.endsWith(".png", true) || clip.mediaUri.endsWith(".jpg", true)
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(clip.mediaUri))
                .apply {
                    if (isImg) {
                        setImageDurationMs(clip.activeDurationMs.coerceAtLeast(1000L))
                    } else {
                        setClippingConfiguration(clipping)
                    }
                }
                .build()

            val videoEffects = mutableListOf<Effect>()
            val exportSpeed = if (clip.speedRamp != com.aistudio.kbeditor.r7v4qx.model.SpeedRampPreset.CONSTANT) {
                listOf(0.15f, 0.35f, 0.5f, 0.7f, 0.9f).map {
                    com.aistudio.kbeditor.r7v4qx.vfx.CurveEvaluators.evaluateSpeedMultiplier(clip.speedRamp, it)
                }.average().toFloat().coerceIn(0.25f, 4.0f)
            } else {
                clip.speedMultiplier.coerceIn(0.1f, 50f)
            }
            if (!isImg && kotlin.math.abs(exportSpeed - 1f) > 0.02f) {
                videoEffects.add(SpeedChangeEffect(exportSpeed))
            }
            videoEffects.add(Presentation.createForWidthAndHeight(targetWidth, targetHeight, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            val wmCrop = if (clip.watermarkRemover.enabled && clip.watermarkRemover.cropMode) 1.14f else 1f
            val kfMid = if (clip.keyframes.isNotEmpty()) {
                com.aistudio.kbeditor.r7v4qx.vfx.CurveEvaluators.interpolateKeyframes(clip.keyframes, 0.5f)
            } else null
            val baseZoom = (kfMid?.scale ?: clip.zoomScale).coerceIn(0.05f, 10.0f)
            val rotZ = clip.rotationDeg + (kfMid?.rotationDeg ?: 0f)
            val finalScale = wmCrop * baseZoom
            if (clip.flipHorizontal || rotZ != 0f || kotlin.math.abs(finalScale - 1f) > 0.01f) {
                videoEffects.add(
                    ScaleAndRotateTransformation.Builder()
                        .setScale((if (clip.flipHorizontal) -1f else 1f) * finalScale, finalScale)
                        .setRotationDegrees(-rotZ)
                        .build()
                )
            }
            val cg = clip.colorGrade
            val hasColorEdit = kotlin.math.abs(cg.brightness) > 0.01f ||
                kotlin.math.abs(cg.contrast - 1f) > 0.01f ||
                kotlin.math.abs(cg.saturation - 1f) > 0.01f ||
                kotlin.math.abs(cg.warmth) > 0.01f ||
                kotlin.math.abs(cg.vibrance) > 0.01f
            if (hasColorEdit) {
                videoEffects.add(buildRgbAdjustment(cg))
            }
            val g = clip.colorGrade
            if (g.vignette > 0.01f || kotlin.math.abs(g.highlights) > 0.01f || kotlin.math.abs(g.shadows) > 0.01f || g.beautyGlow > 0.01f) {
                videoEffects.add(CinematicGradeGlEffect(g.vignette, g.highlights, g.shadows, g.beautyGlow))
            }
            if (clip.videoFx != VideoFxPreset.NONE) {
                videoEffects.add(VideoFxGlEffect(clip.videoFx, clip.videoFxIntensity, clip.videoFxSpeed, clip.videoFxColorShift))
            }
            if (clip.maskType != MaskType.NONE) {
                videoEffects.add(MaskGlEffect(clip.maskType, clip.maskRotationDeg, clip.maskInverted, clip.maskScale, clip.maskOffsetX, clip.maskOffsetY, clip.maskScaleX, clip.maskScaleY, clip.maskFeather))
            }
            if (clip.chromaKey.enabled) {
                videoEffects.add(
                    ChromaKeyGlEffect(
                        keyHue = clip.chromaKey.keyHue,
                        tolerance = clip.chromaKey.tolerance,
                        spillSuppression = clip.chromaKey.spillSuppression,
                        backdropColorHex = clip.chromaKey.studioBackdropHex
                    )
                )
            }

            val overlays = mutableListOf<BitmapOverlay>()
            if (clip.chromaKey.aiAutoCut) {
                val cutFrames = withContext(Dispatchers.IO) {
                    buildAiAutoCutFrames(clip.mediaUri, clip.trimStartMs, clip.trimEndMs, clip.chromaKey.studioBackdropHex)
                }
                if (cutFrames.isNotEmpty()) {
                    overlays.add(object : BitmapOverlay() {
                        override fun getBitmap(presentationTimeUs: Long): Bitmap {
                            val ms = presentationTimeUs / 1000L
                            return cutFrames.minByOrNull { kotlin.math.abs(it.first - ms) }?.second ?: cutFrames.first().second
                        }
                    })
                }
            }
            if (clip.faceSwap.enabled && clip.faceSwap.sourceFaceUri.isNotBlank()) {
                val faceFrames = withContext(Dispatchers.IO) {
                    buildFaceSwapFrames(clip.mediaUri, clip.trimStartMs, clip.trimEndMs, clip.faceSwap, targetWidth, targetHeight)
                }
                if (faceFrames.isNotEmpty()) {
                    overlays.add(object : BitmapOverlay() {
                        override fun getBitmap(presentationTimeUs: Long): Bitmap {
                            val ms = presentationTimeUs / 1000L
                            return faceFrames.minByOrNull { kotlin.math.abs(it.first - ms) }?.second ?: faceFrames.first().second
                        }
                    })
                }
            }
            if (project.tracks.o1Visible && project.pipClips.isNotEmpty()) {
                val pipBitmaps = withContext(Dispatchers.IO) {
                    project.pipClips.associate { pip ->
                        pip.id to loadPipBitmap(pip.mediaUri)
                    }
                }
                val ovScale = if (targetWidth > 1080) 1080f / targetWidth.toFloat() else 1f
                val ovW = (targetWidth * ovScale).toInt().coerceAtLeast(360)
                val ovH = (targetHeight * ovScale).toInt().coerceAtLeast(360)
                val emptyPipBmp = Bitmap.createBitmap(ovW, ovH, Bitmap.Config.ARGB_8888)
                var cachedPipKey = -1L
                var cachedPipBmp: Bitmap = emptyPipBmp
                overlays.add(object : BitmapOverlay() {
                    override fun getBitmap(presentationTimeUs: Long): Bitmap {
                        val bucket = ((presentationTimeUs / 1000L) + clipStartInProjectMs) / 200L
                        if (bucket != cachedPipKey) {
                            cachedPipKey = bucket
                            cachedPipBmp = renderPipOverlayBitmapAt(project, pipBitmaps, ovW, ovH, bucket * 200L) ?: emptyPipBmp
                        }
                        return cachedPipBmp
                    }
                })
            }
            if (project.tracks.t1Visible && project.captionClips.isNotEmpty()) {
                val capScale = if (targetWidth > 1080) 1080f / targetWidth.toFloat() else 1f
                val capW = (targetWidth * capScale).toInt().coerceAtLeast(360)
                val capH = (targetHeight * capScale).toInt().coerceAtLeast(360)
                val emptyBmp = Bitmap.createBitmap(capW, capH, Bitmap.Config.ARGB_8888)
                var cachedCapKey = -1L
                var cachedCapBmp: Bitmap = emptyBmp
                overlays.add(object : BitmapOverlay() {
                    override fun getBitmap(presentationTimeUs: Long): Bitmap {
                        val bucket = ((presentationTimeUs / 1000L) + clipStartInProjectMs) / 150L
                        if (bucket != cachedCapKey) {
                            cachedCapKey = bucket
                            cachedCapBmp = renderCaptionOverlayBitmapAt(project, capW, capH, bucket * 150L) ?: emptyBmp
                        }
                        return cachedCapBmp
                    }
                })
            }
            if (clip.watermarkRemover.enabled && !clip.watermarkRemover.cropMode) {
                val wm = clip.watermarkRemover
                val wmBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val c = Canvas(wmBmp)
                val bw = targetWidth * wm.boxWidth
                val bh = targetHeight * wm.boxHeight
                val left = (targetWidth * wm.posX - bw / 2f).coerceIn(0f, targetWidth - bw)
                val top = (targetHeight * wm.posY - bh / 2f).coerceIn(0f, targetHeight - bh)
                val pPatch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    shader = RadialGradient(
                        left + bw / 2f, top + bh / 2f, maxOf(bw, bh).coerceAtLeast(16f),
                        intArrayOf(Color.argb(140, 180, 190, 205), Color.argb(80, 140, 150, 165), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP
                    )
                }
                c.drawRoundRect(RectF(left, top, left + bw, top + bh), 24f, 24f, pPatch)
                overlays.add(BitmapOverlay.createStaticBitmapOverlay(wmBmp))
            }

            val fxLabelLow = clip.videoFxLabel.ifBlank { clip.videoFx.label }.lowercase()
            val needsParticleOverlay = clip.videoFx != VideoFxPreset.NONE && listOf(
                "thunder", "storm", "lightning", "rain", "flower", "petal", "sakura",
                "star", "blink", "sparkle", "glitter", "snow", "fire", "ember", "heart", "love"
            ).any { it in fxLabelLow }
            if (needsParticleOverlay) {
                val pScale = if (targetWidth > 720) 720f / targetWidth.toFloat() else 1f
                val pw = (targetWidth * pScale).toInt().coerceAtLeast(360)
                val ph = (targetHeight * pScale).toInt().coerceAtLeast(360)
                val pBmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                val pCan = Canvas(pBmp)
                val pPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                var lastPKey = -1L
                overlays.add(object : BitmapOverlay() {
                    override fun getBitmap(presentationTimeUs: Long): Bitmap {
                        val ms = presentationTimeUs / 1000L
                        val bucket = ms / 120L
                        if (bucket != lastPKey) {
                            lastPKey = bucket
                            pCan.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            val fxT = (ms / 1000f) * clip.videoFxSpeed.coerceIn(0.25f, 10f)
                            when {
                                "thunder" in fxLabelLow || "storm" in fxLabelLow || "lightning" in fxLabelLow || "rain" in fxLabelLow -> {
                                    if ((bucket % 7L) == 0L) pCan.drawColor(Color.argb(95, 224, 242, 254))
                                    pPaint.color = Color.argb(150, 186, 230, 253)
                                    pPaint.strokeWidth = 3f
                                    for (r in 0 until 20) {
                                        val rx = ((r * 53 + (fxT * 180).toInt()) % 100) / 100f * pw
                                        val ry = ((r * 37 + (fxT * 420).toInt()) % 100) / 100f * ph
                                        pCan.drawLine(rx, ry, rx - 10f, ry + 34f, pPaint)
                                    }
                                }
                                "flower" in fxLabelLow || "petal" in fxLabelLow || "sakura" in fxLabelLow -> {
                                    for (p in 0 until 14) {
                                        val px = (((p * 67) % 100) / 100f + kotlin.math.sin(fxT * 2.0 + p).toFloat() * 0.08f) * pw
                                        val py = (((p * 41 + (fxT * 65).toInt()) % 100) / 100f) * ph
                                        pPaint.color = if (p % 2 == 0) Color.argb(215, 244, 114, 182) else Color.argb(215, 253, 164, 175)
                                        pCan.drawCircle(px, py, 8f + (p % 4) * 2.5f, pPaint)
                                    }
                                }
                                "star" in fxLabelLow || "blink" in fxLabelLow || "sparkle" in fxLabelLow || "glitter" in fxLabelLow -> {
                                    pPaint.strokeWidth = 3f
                                    for (s in 0 until 16) {
                                        val sx = ((s * 59 + 13) % 94 + 3) / 100f * pw
                                        val sy = ((s * 43 + 17) % 92 + 4) / 100f * ph
                                        val pulse = kotlin.math.abs(kotlin.math.sin(fxT * 4.5 + s)).toFloat().coerceIn(0.25f, 1f)
                                        val arm = 12f * pulse
                                        pPaint.color = if (s % 3 == 0) Color.argb((pulse * 255).toInt(), 253, 224, 71) else Color.argb((pulse * 255).toInt(), 255, 255, 255)
                                        pCan.drawLine(sx - arm, sy, sx + arm, sy, pPaint)
                                        pCan.drawLine(sx, sy - arm, sx, sy + arm, pPaint)
                                    }
                                }
                                "snow" in fxLabelLow -> {
                                    pPaint.color = Color.argb(215, 255, 255, 255)
                                    for (f in 0 until 20) {
                                        val sx = (((f * 47) % 100) / 100f + kotlin.math.sin(fxT * 1.5 + f).toFloat() * 0.05f) * pw
                                        val sy = (((f * 29 + (fxT * 45).toInt()) % 100) / 100f) * ph
                                        pCan.drawCircle(sx, sy, 4f + (f % 3) * 2f, pPaint)
                                    }
                                }
                                "fire" in fxLabelLow || "ember" in fxLabelLow -> {
                                    pPaint.color = Color.argb(220, 251, 146, 60)
                                    for (e in 0 until 16) {
                                        val ex = ((e * 61) % 100) / 100f * pw
                                        val ey = ph - (((e * 31 + (fxT * 90).toInt()) % 100) / 100f) * ph
                                        pCan.drawCircle(ex, ey, 5f + (e % 3) * 2f, pPaint)
                                    }
                                }
                                else -> {
                                    pPaint.color = Color.argb(210, 244, 63, 94)
                                    for (h in 0 until 12) {
                                        val hx = ((h * 71) % 90 + 5) / 100f * pw
                                        val hy = ph - (((h * 37 + (fxT * 55).toInt()) % 100) / 100f) * ph
                                        pCan.drawCircle(hx, hy, 8f, pPaint)
                                    }
                                }
                            }
                        }
                        return pBmp
                    }
                })
            }

            if (clip.fadeInMs > 0L || clip.fadeOutMs > 0L) {
                val clipDur = clip.activeDurationMs.coerceAtLeast(500L)
                val fadeBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val fadeCanvas = Canvas(fadeBmp)
                overlays.add(object : BitmapOverlay() {
                    override fun getBitmap(presentationTimeUs: Long): Bitmap {
                        val ms = presentationTimeUs / 1000L
                        val inAlpha = if (clip.fadeInMs > 0L && ms < clip.fadeInMs) 1f - (ms.toFloat() / clip.fadeInMs) else 0f
                        val outRem = clipDur - ms
                        val outAlpha = if (clip.fadeOutMs > 0L && outRem in 0L..clip.fadeOutMs) 1f - (outRem.toFloat() / clip.fadeOutMs) else 0f
                        val a = maxOf(inAlpha, outAlpha).coerceIn(0f, 1f)
                        fadeCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        if (a > 0.02f) {
                            fadeCanvas.drawColor(Color.argb((a * 255).toInt(), 0, 0, 0))
                        }
                        return fadeBmp
                    }
                })
            }
            if (clip.transitionOut != com.aistudio.kbeditor.r7v4qx.model.TransitionType.NONE) {
                val clipDur = clip.activeDurationMs.coerceAtLeast(500L)
                val transDur = clip.transitionDurationMs.coerceIn(200L, clipDur / 2)
                val transBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val transCanvas = Canvas(transBmp)
                overlays.add(object : BitmapOverlay() {
                    override fun getBitmap(presentationTimeUs: Long): Bitmap {
                        val ms = presentationTimeUs / 1000L
                        val rem = clipDur - ms
                        transCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        if (rem in 0L..transDur) {
                            val t = 1f - (rem.toFloat() / transDur.toFloat())
                            val alpha = (t * 235).toInt().coerceIn(0, 255)
                            val col = if (clip.transitionOut == com.aistudio.kbeditor.r7v4qx.model.TransitionType.FLASH_CUT) {
                                Color.argb(alpha, 255, 255, 255)
                            } else {
                                Color.argb(alpha, 0, 0, 0)
                            }
                            transCanvas.drawColor(col)
                        }
                        return transBmp
                    }
                })
            }
            if (overlays.isNotEmpty()) {
                videoEffects.add(OverlayEffect(overlays.toList()))
            }

            // V1 source audio has its own clip volume/mute. A1 mute and A1 DSP
            // must never leak into the original camera audio.
            val audioDsp = StudioAudioDspProcessor().apply {
                outputGain = clip.volume.coerceIn(0f, 1.5f)
            }

            EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(listOf(audioDsp), videoEffects))
                .setRemoveAudio(clip.muted)
                .build()
        }

        val sequence = EditedMediaItemSequence.Builder(editedVideoItems).build()
        val sequences = mutableListOf(sequence)

        // Each A1 clip gets its own parallel sequence. Grouping clips into a
        // single sequence concatenates overlapping clips and shifts their timing.
        val exportAudioClips = if (!project.tracks.a1Muted) {
            project.audioClips.filter { it.mediaUri.isNotBlank() }
        } else emptyList()

        exportAudioClips.forEach { aClip ->
            val audioDsp = StudioAudioDspProcessor(
                windFilterEnabled = aClip.windFilterEnabled,
                noiseGateEnabled = aClip.noiseGateEnabled,
                voiceEnhanceEnabled = aClip.voiceEnhanceEnabled,
                        echoRemovalEnabled = aClip.echoRemovalEnabled,
                vocalMode = aClip.vocalMode
            ).apply {
                outputGain = aClip.volume.coerceIn(0f, 1.5f)
            }
            val audioMediaItem = MediaItem.Builder()
                .setUri(Uri.parse(aClip.mediaUri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0L)
                        .setEndPositionMs(aClip.durationMs.coerceAtLeast(100L))
                        .build()
                )
                .build()
            val aItem = EditedMediaItem.Builder(audioMediaItem)
                .setEffects(Effects(listOf(audioDsp), emptyList()))
                .setRemoveVideo(true)
                .build()
            val audioSeqBuilder = EditedMediaItemSequence.Builder()
            if (aClip.startMs > 0L) audioSeqBuilder.addGap(aClip.startMs * 1000L)
            audioSeqBuilder.addItem(aItem)
            sequences.add(audioSeqBuilder.build())
        }

        val hdrMode = if (project.exportSettings.dynamicRange == ExportDynamicRange.HDR10_HLG) {
            Composition.HDR_MODE_KEEP_HDR
        } else {
            Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
        }

        val composition = Composition.Builder(sequences)
            .setHdrMode(hdrMode)
            .build()

        val videoMime = if (
            project.exportSettings.dynamicRange == ExportDynamicRange.HDR10_HLG ||
            project.exportSettings.resolution == ExportResolution.UHD_4K
        ) {
            MimeTypes.VIDEO_H265
        } else {
            MimeTypes.VIDEO_H264
        }

        val tuning = DeviceEngineProfile.activeTuning
        val exportBitrate = when (project.exportSettings.resolution) {
            ExportResolution.UHD_4K -> tuning.bitrate4k
            ExportResolution.FHD_1080 -> tuning.bitrate1080p
            ExportResolution.HD_720 -> tuning.bitrate720p
        }
        val result = runTransformer(composition, outFile, videoMime, onProgress, exportBitrate)
        if (result.isFailure) {
            // Safe H.264 SDR fallback with primary sequence & balanced bitrate for strict MediaCodec devices
            if (outFile.exists()) outFile.delete()
            // Preserve parallel A1/voiceover/SFX sequences in the fallback export.
            val fallbackComp = Composition.Builder(sequences)
                .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
                .build()
            val fallbackResult = runTransformer(
                fallbackComp,
                outFile,
                MimeTypes.VIDEO_H264,
                onProgress,
                exportBitrate.coerceAtMost(14_000_000)
            )
            if (fallbackResult.isFailure) return@withContext fallbackResult
        }

        val savedPath = withContext(Dispatchers.IO) {
            saveToMoviesKbEditor(outFile)
        }
        Result.success(savedPath)
    }

    private suspend fun runTransformer(
        composition: Composition,
        outFile: File,
        videoMime: String,
        onProgress: (Float) -> Unit,
        targetBitrate: Int = 22_000_000
    ): Result<String> = suspendCancellableCoroutine { cont ->
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetBitrate)
                    .setBitrateMode(android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                    .build()
            )
            .build()
        val transformer = Transformer.Builder(context)
            .setEncoderFactory(encoderFactory)
            .setVideoMimeType(videoMime)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onProgress(1f)
                    if (cont.isActive) cont.resume(Result.success(outFile.absolutePath))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (cont.isActive) cont.resume(Result.failure(exportException))
                }
            })
            .build()

        transformer.start(composition, outFile.absolutePath)
        cont.invokeOnCancellation {
            transformer.cancel()
        }
    }


    private suspend fun buildAiAutoCutFrames(
        mediaUri: String,
        startMs: Long,
        endMs: Long,
        backdropHex: Long
    ): List<Pair<Long, Bitmap>> {
        val engine = AiAutoCutEngine()
        val retriever = MediaMetadataRetriever()
        val out = mutableListOf<Pair<Long, Bitmap>>()
        try {
            retriever.setDataSource(context, Uri.parse(mediaUri))
            val dur = (endMs - startMs).coerceAtLeast(500L)
            val steps = 12
            for (i in 0..steps) {
                val relMs = (dur * i) / steps
                val frame = retriever.getFrameAtTime((startMs + relMs) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val small = Bitmap.createScaledBitmap(frame, 270, 480, true)
                    if (small !== frame) frame.recycle()
                    val seg = engine.segmentForeground(small, backdropHex)
                    small.recycle()
                    if (seg != null) out.add(relMs to seg)
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
            engine.close()
        }
        return out
    }

    private suspend fun buildFaceSwapFrames(
        mediaUri: String,
        startMs: Long,
        endMs: Long,
        spec: com.aistudio.kbeditor.r7v4qx.model.FaceSwapSpec,
        targetW: Int,
        targetH: Int
    ): List<Pair<Long, Bitmap>> {
        val engine = FaceSwapEngine(context)
        val profile = engine.prepareSourceFaceProfile(spec.sourceFaceUri) ?: run {
            engine.close()
            return emptyList()
        }
        val retriever = MediaMetadataRetriever()
        val out = mutableListOf<Pair<Long, Bitmap>>()
        try {
            retriever.setDataSource(context, Uri.parse(mediaUri))
            val dur = (endMs - startMs).coerceAtLeast(500L)
            val steps = 12
            for (i in 0..steps) {
                val relMs = (dur * i) / steps
                val frame = retriever.getFrameAtTime((startMs + relMs) * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val overlayBmp = engine.renderLandmarkFaceOverlay(
                        targetFrame = frame,
                        source = profile,
                        outW = targetW,
                        outH = targetH,
                        scaleMultiplier = spec.scaleMultiplier,
                        blendOpacity = spec.blendOpacity
                    )
                    frame.recycle()
                    if (overlayBmp != null) {
                        out.add(relMs to overlayBmp)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            profile.bitmap.recycle()
            runCatching { retriever.release() }
            engine.close()
        }
        return out
    }

    private fun buildRgbAdjustment(grade: ColorGradeSpec): RgbAdjustment {
        val rScale = (grade.contrast * (1f + grade.gain.dx * 0.25f + grade.warmth * 0.15f)).coerceIn(0.4f, 2.2f)
        val gScale = (grade.contrast * (1f + grade.gamma.dy * 0.2f)).coerceIn(0.4f, 2.2f)
        val bScale = (grade.contrast * (1f - grade.gain.dx * 0.25f - grade.warmth * 0.15f)).coerceIn(0.4f, 2.2f)
        return RgbAdjustment.Builder()
            .setRedScale(rScale)
            .setGreenScale(gScale)
            .setBlueScale(bScale)
            .build()
    }

    private fun loadPipBitmap(mediaUri: String): Bitmap? {
        if (mediaUri.isBlank()) return null
        val fromStream = runCatching {
            context.contentResolver.openInputStream(Uri.parse(mediaUri))?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
        if (fromStream != null) return fromStream
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(mediaUri))
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun renderPipOverlayBitmapAt(
        project: KbEditorProject,
        pipBitmaps: Map<String, Bitmap?>,
        width: Int,
        height: Int,
        timeMs: Long
    ): Bitmap? {
        val active = project.pipClips.filter { timeMs in it.startMs..(it.startMs + it.durationMs) }
        if (active.isEmpty()) return null
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        active.forEach { pip ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (pip.opacity.coerceIn(0.1f, 1f) * 255).toInt()
            }
            val cx = width * pip.posX
            val cy = height * pip.posY
            val bmp = pipBitmaps[pip.id]
            if (bmp != null) {
                val boxW = width * pip.scale.coerceIn(0.2f, 0.9f)
                val boxH = boxW * (bmp.height.toFloat() / bmp.width.coerceAtLeast(1))
                canvas.save()
                canvas.rotate(pip.rotationDeg, cx, cy)
                val dst = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)
                canvas.drawBitmap(bmp, null, dst, paint)
                canvas.restore()
            } else if (pip.opticalFxPreset?.startsWith("STICKER:") == true) {
                val label = pip.opticalFxPreset.removePrefix("STICKER:")
                val tSize = (width * 0.055f * pip.scale.coerceIn(0.5f, 1.4f)).coerceIn(28f, 120f)
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = tSize
                    color = Color.WHITE
                }
                val tw = tp.measureText(label)
                val rect = RectF(cx - tw / 2f - 28f, cy - tSize - 10f, cx + tw / 2f + 28f, cy + 20f)
                val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(215, 17, 24, 39) }
                val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    color = Color.parseColor("#F59E0B")
                }
                canvas.drawRoundRect(rect, 24f, 24f, bp)
                canvas.drawRoundRect(rect, 24f, 24f, sp)
                canvas.drawText(label, cx, cy, tp)
            } else {
                val r = minOf(width, height) * 0.28f * pip.scale.coerceIn(0.2f, 1.2f)
                paint.shader = RadialGradient(
                    cx, cy, r.coerceAtLeast(10f),
                    intArrayOf(Color.argb(paint.alpha, 34, 211, 238), Color.argb(90, 245, 158, 11), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy, r, paint)
            }
        }
        return out
    }

    private fun renderCaptionOverlayBitmapAt(
        project: KbEditorProject,
        width: Int,
        height: Int,
        timeMs: Long
    ): Bitmap? {
        val active = project.captionClips.filter { timeMs in it.startMs..(it.startMs + it.durationMs) }
        if (active.isEmpty()) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        active.take(2).forEach { cap ->
            val txt = if (cap.stylePreset == CaptionStylePreset.VIRAL_HORMOZI || cap.stylePreset == CaptionStylePreset.MINIMAL_TAG) {
                cap.text.uppercase()
            } else cap.text
            val textSizePx = (cap.fontSizeSp * (width / 360f)).coerceIn(28f, 140f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(
                    when (cap.fontStyle) {
                        CaptionFontStyle.SERIF_EDITORIAL -> Typeface.SERIF
                        CaptionFontStyle.MONO_CODE -> Typeface.MONOSPACE
                        else -> Typeface.SANS_SERIF
                    },
                    if (cap.isBold) Typeface.BOLD else Typeface.NORMAL
                )
                textSize = textSizePx
                color = when (cap.stylePreset) {
                    CaptionStylePreset.MINIMAL_TAG -> Color.parseColor("#090B10")
                    CaptionStylePreset.VIRAL_HORMOZI, CaptionStylePreset.GOLD_LUXURY -> cap.accentHex.toInt()
                    else -> cap.colorHex.toInt()
                }
                setShadowLayer(8f, 0f, 3f, if (cap.stylePreset == CaptionStylePreset.NEON_CYBER) Color.CYAN else Color.argb(200, 0, 0, 0))
            }

            val cx = width * cap.posX
            val cy = height * cap.posY
            val textW = textPaint.measureText(txt)
            val padX = textSizePx * 0.65f
            val padY = textSizePx * 0.45f
            val boxRect = RectF(
                (cx - textW / 2f - padX).coerceAtLeast(16f),
                cy - textSizePx - padY * 0.4f,
                (cx + textW / 2f + padX).coerceAtMost(width - 16f),
                cy + padY
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (width / 240f).coerceAtLeast(3f)
            }

            canvas.save()
            canvas.rotate(cap.rotationDeg, cx, cy)
            when (cap.stylePreset) {
                CaptionStylePreset.VIRAL_HORMOZI -> {
                    bgPaint.color = Color.argb(135, 0, 0, 0)
                    canvas.drawRoundRect(boxRect, 20f, 20f, bgPaint)
                    strokePaint.color = cap.accentHex.toInt()
                    canvas.drawRoundRect(boxRect, 20f, 20f, strokePaint)
                }
                CaptionStylePreset.NEON_CYBER -> {
                    bgPaint.color = Color.argb(205, 6, 9, 19)
                    canvas.drawRoundRect(boxRect, 24f, 24f, bgPaint)
                    strokePaint.color = Color.parseColor("#22D3EE")
                    canvas.drawRoundRect(boxRect, 24f, 24f, strokePaint)
                }
                CaptionStylePreset.DARK_PILL -> {
                    bgPaint.color = Color.argb(220, 9, 11, 16)
                    canvas.drawRoundRect(boxRect, 48f, 48f, bgPaint)
                }
                CaptionStylePreset.GRADIENT_POP -> {
                    bgPaint.shader = LinearGradient(
                        boxRect.left, boxRect.top, boxRect.right, boxRect.bottom,
                        intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#EC4899"), Color.parseColor("#F59E0B")),
                        null, Shader.TileMode.CLAMP
                    )
                    canvas.drawRoundRect(boxRect, 28f, 28f, bgPaint)
                }
                CaptionStylePreset.GOLD_LUXURY -> {
                    bgPaint.color = Color.argb(220, 11, 14, 20)
                    canvas.drawRoundRect(boxRect, 20f, 20f, bgPaint)
                    strokePaint.color = Color.parseColor("#F59E0B")
                    canvas.drawRoundRect(boxRect, 20f, 20f, strokePaint)
                }
                CaptionStylePreset.OUTLINE_IMPACT -> Unit
                CaptionStylePreset.CINEMA_SUB -> {
                    bgPaint.color = Color.argb(70, 0, 0, 0)
                    canvas.drawRoundRect(boxRect, 12f, 12f, bgPaint)
                }
                CaptionStylePreset.MINIMAL_TAG -> {
                    bgPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawRoundRect(boxRect, 16f, 16f, bgPaint)
                }
            }

            canvas.drawText(txt, cx, cy, textPaint)
            canvas.restore()
        }
        return bitmap
    }

    private fun computeTargetWidth(project: KbEditorProject): Int {
        val base = project.exportSettings.resolution.heightPx
        val ar = project.aspectRatio.widthRatio / project.aspectRatio.heightRatio
        val raw = if (ar >= 1f) (base * ar).toInt() else base
        return raw - (raw % 2)
    }

    private fun computeTargetHeight(project: KbEditorProject): Int {
        val base = project.exportSettings.resolution.heightPx
        val ar = project.aspectRatio.widthRatio / project.aspectRatio.heightRatio
        val raw = if (ar >= 1f) base else (base / ar).toInt()
        return raw - (raw % 2)
    }

    private fun saveToMoviesKbEditor(sourceFile: File): String {
        val displayName = "KB_Editor_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/KB Editor")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                runCatching { sourceFile.delete() }
                return "Movies/KB Editor/$displayName"
            }
        }
        val moviesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "KB Editor"
        ).apply { mkdirs() }
        val dest = File(moviesDir, displayName)
        sourceFile.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }
}
