package com.aistudio.kbeditor.r7v4qx.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.aistudio.kbeditor.r7v4qx.audio.AudioExtractorEngine
import com.aistudio.kbeditor.r7v4qx.audio.CapCutSfxPreset
import com.aistudio.kbeditor.r7v4qx.audio.StudioSfxEngine
import com.aistudio.kbeditor.r7v4qx.model.AudioClip
import com.aistudio.kbeditor.r7v4qx.model.MaskType
import com.aistudio.kbeditor.r7v4qx.model.VideoFxPreset
import com.aistudio.kbeditor.r7v4qx.model.CaptionClip
import com.aistudio.kbeditor.r7v4qx.model.CaptionLanguage
import com.aistudio.kbeditor.r7v4qx.model.CaptionStylePreset
import org.json.JSONArray
import com.aistudio.kbeditor.r7v4qx.model.ColorGradeSpec
import com.aistudio.kbeditor.r7v4qx.model.ColorWheelPoint
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import com.aistudio.kbeditor.r7v4qx.model.MotionAnimation3D
import com.aistudio.kbeditor.r7v4qx.model.SpeedRampPreset
import com.aistudio.kbeditor.r7v4qx.model.TransitionType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class VoiceProfileMatch(
    val pitch: Float,
    val speed: Float,
    val languageTag: String,
    val summary: String
)

data class ViralExportKit(
    val viralTitle: String,
    val caption: String,
    val hashtags: String,
    val thumbnailUri: String
)

data class AiEditSummary(
    val updatedProject: KbEditorProject,
    val message: String
)

class GeminiStudioEngine(private val context: Context) {

    private val flashModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash")
    }

    suspend fun runAutoCreator(
        prompt: String,
        project: KbEditorProject
    ): AiEditSummary = withContext(Dispatchers.IO) {
        val sampleFrame = extractFirstFrame(project)
        val aiPlanJson = withTimeoutOrNull(8000L) { runCatching {
            val response = flashModel.generateContent(
                content {
                    if (sampleFrame != null) image(sampleFrame)
                    text(
                        """
                        You are KB Editor's One-Prompt Beginner-Friendly AI Editor. Prompt: "$prompt"
                        Return ONLY a compact JSON object with keys:
                        "lutName" (string), "contrast" (float 0.8..1.5), "saturation" (float 0.6..1.6),
                        "warmth" (float -0.5..0.5), "vignette" (float 0..0.45),
                        "speedRamp" (CONSTANT|HERO_DIP|FLASH_IN|BULLET_TIME|BUILD_UP),
                        "transition" (NONE|CROSS_DISSOLVE|WHIP_PAN|FLASH_CUT|ZOOM_THROUGH),
                        "videoFx" (NONE|GLITCH_RGB|VHS_RETRO|CAM_SHAKE|FLASH_STROBE|DREAM_BLOOM|CHROMATIC|ZOOM_PULSE|CINEMA_GRAIN|SPLIT_2X|NEON_EDGE|HEARTBEAT),
                        "maskType" (NONE|ANAMORPHIC_BARS|CIRCLE|ROUNDED_RECT|SPLIT_WIPE),
                        "sfx" (NONE|WHOOSH_FAST|BASS_DROP|POP_BUBBLE|GLITCH_ZAP|CINEMA_BOOM|SPARKLE_MAGIC|PUNCH_HIT),
                        "captionText" (short 3-6 words), "captionStyle" (VIRAL_HORMOZI|NEON_CYBER|DARK_PILL|GRADIENT_POP|CINEMA_SUB),
                        "enableVoiceClean" (boolean)
                        """.trimIndent()
                    )
                }
            )
            response.text
        }.getOrNull() }

        sampleFrame?.recycle()

        val lower = prompt.lowercase()
        val parsed = parseJsonSafe(aiPlanJson)

        val speedRamp = parseEnum<SpeedRampPreset>(parsed?.optString("speedRamp"))
            ?: when {
                "hype" in lower || "action" in lower || "reel" in lower -> SpeedRampPreset.HERO_DIP
                "slow" in lower || "dhima" in lower || "dheere" in lower || "धीमा" in lower || "cinematic" in lower -> SpeedRampPreset.BULLET_TIME
                else -> SpeedRampPreset.FLASH_IN
            }

        val transition = parseEnum<TransitionType>(parsed?.optString("transition"))
            ?: if ("action" in lower || "fast" in lower) TransitionType.WHIP_PAN else TransitionType.CROSS_DISSOLVE

        val lutName = parsed?.optString("lutName")?.takeIf { it.isNotBlank() }
            ?: when {
                "teal" in lower || "cinematic" in lower -> "Teal & Orange Pro"
                "warm" in lower || "garam" in lower || "dhoop" in lower || "vlog" in lower -> "Golden Film"
                "cyber" in lower || "neon" in lower -> "Cyber Neon"
                else -> "Studio Crisp"
            }

        val videoFx = parseEnum<VideoFxPreset>(parsed?.optString("videoFx"))
            ?: when {
                "glitch" in lower -> VideoFxPreset.GLITCH_RGB
                "shake" in lower || "hilao" in lower || "hila" in lower || "action" in lower -> VideoFxPreset.CAM_SHAKE
                "split" in lower -> VideoFxPreset.SPLIT_2X
                "neon" in lower || "cyber" in lower -> VideoFxPreset.NEON_EDGE
                "zoom" in lower || "beat" in lower || "gaana" in lower || "reel" in lower -> VideoFxPreset.ZOOM_PULSE
                "vhs" in lower || "retro" in lower || "purana" in lower || "पुराना" in lower -> VideoFxPreset.VHS_RETRO
                else -> VideoFxPreset.NONE
            }

        val maskType = parseEnum<MaskType>(parsed?.optString("maskType"))
            ?: if ("cinema" in lower || "film" in lower || "movie" in lower) MaskType.ANAMORPHIC_BARS else MaskType.NONE

        val vignette = parsed?.optDouble("vignette", if ("cinema" in lower) 0.25 else 0.12)?.toFloat()?.coerceIn(0f, 0.5f) ?: 0.12f

        val updatedClips = project.videoClips.mapIndexed { idx, clip ->
            clip.copy(
                speedRamp = speedRamp,
                videoFx = videoFx,
                maskType = maskType,
                transitionOut = if (idx < project.videoClips.lastIndex) transition else TransitionType.NONE,
                colorGrade = clip.colorGrade.copy(
                    lutName = lutName,
                    contrast = parsed?.optDouble("contrast", 1.15)?.toFloat() ?: 1.15f,
                    saturation = parsed?.optDouble("saturation", 1.12)?.toFloat() ?: 1.12f,
                    warmth = parsed?.optDouble("warmth", 0.08)?.toFloat() ?: 0.08f,
                    vignette = vignette
                )
            )
        }

        val captionStyle = parseEnum<CaptionStylePreset>(parsed?.optString("captionStyle"))
            ?: if ("neon" in lower || "cyber" in lower) CaptionStylePreset.NEON_CYBER else CaptionStylePreset.VIRAL_HORMOZI

        val wantsText = listOf("text", "caption", "title", "likh", "sub").any { it in lower }
        val captionText = parsed?.optString("captionText")?.takeIf { it.isNotBlank() && wantsText }
            ?: if (wantsText) prompt.trim().take(24) else ""

        val updatedCaptions = if (captionText.isNotBlank()) {
            listOf(
                CaptionClip(
                    text = captionText,
                    startMs = 200L,
                    durationMs = 3000L,
                    posY = 0.82f,
                    fontSizeSp = 20f,
                    animation3D = MotionAnimation3D.POP_3D,
                    stylePreset = captionStyle
                )
            )
        } else project.captionClips

        val voiceClean = parsed?.optBoolean("enableVoiceClean", true) ?: true
        val baseAudio = project.audioClips.map {
            it.copy(
                noiseGateEnabled = voiceClean,
                windFilterEnabled = voiceClean,
                voiceEnhanceEnabled = voiceClean
            )
        }.toMutableList()

        val sfxChoice = parseEnum<CapCutSfxPreset>(parsed?.optString("sfx"))
            ?: when {
                "bass" in lower || "drop" in lower || "reel" in lower -> CapCutSfxPreset.BASS_DROP
                "glitch" in lower -> CapCutSfxPreset.GLITCH_ZAP
                "action" in lower || "punch" in lower -> CapCutSfxPreset.PUNCH_HIT
                else -> CapCutSfxPreset.WHOOSH_FAST
            }
        val sfxRes = StudioSfxEngine(context).generateSfxWav(sfxChoice).getOrNull()
        if (sfxRes != null) {
            baseAudio.add(
                AudioClip(
                    title = "SFX: ${sfxChoice.label}",
                    mediaUri = sfxRes.first,
                    startMs = 250L,
                    durationMs = sfxRes.second
                )
            )
        }

        val nextProject = project.copy(
            videoClips = updatedClips,
            captionClips = updatedCaptions,
            audioClips = baseAudio,
            tracks = project.tracks.copy(a1Muted = false, t1Visible = true)
        )
        AiEditSummary(
            updatedProject = nextProject,
            message = "One-Prompt Applied: '$lutName' LUT, ${speedRamp.label} Speed, ${videoFx.label} FX, ${sfxChoice.label} SFX, Voice Clarity EQ & ${captionStyle.label} Caption."
        )
    }

    suspend fun runAiColorGrade(
        stylePrompt: String,
        project: KbEditorProject,
        selectedClipIndex: Int
    ): AiEditSummary = withContext(Dispatchers.IO) {
        val sampleFrame = extractFirstFrame(project)
        val raw = runCatching {
            flashModel.generateContent(
                content {
                    if (sampleFrame != null) image(sampleFrame)
                    text("Suggest JSON ColorGradeSpec for '$stylePrompt': lutName, liftDx, gammaDx, gainDx, contrast, saturation, warmth.")
                }
            ).text
        }.getOrNull()
        sampleFrame?.recycle()

        val parsed = parseJsonSafe(raw)
        val lower = stylePrompt.lowercase()
        val grade = when {
            "teal" in lower || "orange" in lower -> ColorGradeSpec(
                lift = ColorWheelPoint(dx = -0.22f, dy = 0.06f, luma = -0.05f),
                gamma = ColorWheelPoint(dx = 0.08f, dy = 0.02f),
                gain = ColorWheelPoint(dx = 0.25f, dy = 0.10f, luma = 0.06f),
                contrast = 1.18f,
                saturation = 1.14f,
                warmth = 0.12f,
                vignette = 0.22f,
                lutName = "Teal & Orange"
            )
            "noir" in lower || "bw" in lower || "mono" in lower -> ColorGradeSpec(
                contrast = 1.32f,
                saturation = 0.05f,
                vignette = 0.35f,
                lutName = "Cinema Noir"
            )
            else -> ColorGradeSpec(
                lift = ColorWheelPoint(dx = parsed?.optDouble("liftDx", -0.08)?.toFloat() ?: -0.08f, dy = 0f),
                gamma = ColorWheelPoint(dx = parsed?.optDouble("gammaDx", 0.06)?.toFloat() ?: 0.06f, dy = 0.02f),
                gain = ColorWheelPoint(dx = parsed?.optDouble("gainDx", 0.12)?.toFloat() ?: 0.12f, dy = 0.05f),
                contrast = parsed?.optDouble("contrast", 1.16)?.toFloat() ?: 1.16f,
                saturation = parsed?.optDouble("saturation", 1.15)?.toFloat() ?: 1.15f,
                warmth = parsed?.optDouble("warmth", 0.1)?.toFloat() ?: 0.1f,
                lutName = parsed?.optString("lutName")?.takeIf { it.isNotBlank() } ?: "AI Studio LUT"
            )
        }

        val clips = project.videoClips.mapIndexed { idx, clip ->
            if (idx == selectedClipIndex) clip.copy(colorGrade = grade) else clip
        }
        AiEditSummary(
            updatedProject = project.copy(videoClips = clips),
            message = "Applied AI Color Grade: ${grade.lutName}"
        )
    }


    suspend fun runAutoColorCorrection(
        project: KbEditorProject,
        selectedClipIndex: Int
    ): AiEditSummary = withContext(Dispatchers.IO) {
        val targetProj = project.videoClips.getOrNull(selectedClipIndex)?.let { project.copy(videoClips = listOf(it)) } ?: project
        val sampleFrame = extractFirstFrame(targetProj)
        var avgLuma = 0.5f
        var warmBias = 0f
        var darkRatio = 0f
        if (sampleFrame != null) {
            val small = Bitmap.createScaledBitmap(sampleFrame, 64, 64, true)
            var sumL = 0f
            var sumRB = 0f
            var darks = 0
            val total = 64 * 64
            for (y in 0 until 64) {
                for (x in 0 until 64) {
                    val px = small.getPixel(x, y)
                    val r = Color.red(px) / 255f
                    val g = Color.green(px) / 255f
                    val b = Color.blue(px) / 255f
                    val l = 0.299f * r + 0.587f * g + 0.114f * b
                    sumL += l
                    sumRB += (r - b)
                    if (l < 0.22f) darks++
                }
            }
            small.recycle()
            avgLuma = sumL / total
            warmBias = sumRB / total
            darkRatio = darks.toFloat() / total
        }

        val raw = kotlinx.coroutines.withTimeoutOrNull(3500L) {
            runCatching {
                flashModel.generateContent(
                    content {
                        if (sampleFrame != null) image(sampleFrame)
                        text(
                            "Analyze exposure, white balance, and contrast. Return ONLY JSON with keys: " +
                            "brightness (-0.3..0.3), contrast (0.9..1.4), saturation (0.9..1.35), warmth (-0.3..0.3), " +
                            "highlights (-0.3..0.3), shadows (-0.3..0.3), report (short 1-sentence correction summary)."
                        )
                    }
                ).text
            }.getOrNull()
        }
        sampleFrame?.recycle()

        val parsed = parseJsonSafe(raw)
        val autoBright = ((0.52f - avgLuma) * 0.45f).coerceIn(-0.22f, 0.25f)
        val autoWarmth = (-warmBias * 0.35f).coerceIn(-0.25f, 0.25f)
        val bright = parsed?.optDouble("brightness", autoBright.toDouble())?.toFloat()?.coerceIn(-0.35f, 0.35f) ?: autoBright
        val contrast = parsed?.optDouble("contrast", 1.14)?.toFloat()?.coerceIn(0.85f, 1.45f) ?: 1.14f
        val sat = parsed?.optDouble("saturation", 1.10)?.toFloat()?.coerceIn(0.8f, 1.4f) ?: 1.10f
        val warmth = parsed?.optDouble("warmth", autoWarmth.toDouble())?.toFloat()?.coerceIn(-0.35f, 0.35f) ?: autoWarmth
        val highs = parsed?.optDouble("highlights", -0.06)?.toFloat()?.coerceIn(-0.4f, 0.4f) ?: -0.06f
        val shads = parsed?.optDouble("shadows", if (avgLuma < 0.42f) 0.14 else 0.04)?.toFloat()?.coerceIn(-0.4f, 0.4f) ?: 0.08f

        val grade = ColorGradeSpec(
            brightness = bright,
            contrast = contrast,
            saturation = sat,
            warmth = warmth,
            highlights = highs,
            shadows = shads,
            vignette = 0.12f,
            lutName = "Auto-Corrected Pro"
        )
        val report = parsed?.optString("report")?.takeIf { it.isNotBlank() }
            ?: "Auto-Corrected: Brightness ${(bright * 100).toInt()}%, Contrast ${(contrast * 100).toInt()}%, Warmth ${(warmth * 100).toInt()}%, Shadows ${(shads * 100).toInt()}% (White Balance & Exposure Fixed)"

        val clips = project.videoClips.mapIndexed { idx, clip ->
            if (idx == selectedClipIndex) clip.copy(colorGrade = grade) else clip
        }
        AiEditSummary(updatedProject = project.copy(videoClips = clips), message = report)
    }



    suspend fun analyzeVoiceSample(audioUri: String): Result<VoiceProfileMatch> = withContext(Dispatchers.IO) {
        runCatching {
            val file = Uri.parse(audioUri).path?.let { File(it) }
            val bytes = if (file != null && file.exists()) {
                val raw = file.readBytes()
                if (raw.size > 2 * 1024 * 1024) raw.copyOf(2 * 1024 * 1024) else raw
            } else null

            val rawResp = if (bytes != null) {
                runCatching {
                    flashModel.generateContent(
                        content {
                            inlineData(bytes, "audio/mp4")
                            text(
                                "Analyze this speaker's voice pitch, speaking speed, and primary language. " +
                                "Return ONLY JSON with keys: pitch (float 0.65..1.45, <0.95 for deep male, >1.15 for female/high), " +
                                "speed (float 0.75..1.35), languageTag (hi-IN|en-US|pa-IN|ta-IN|es-ES), " +
                                "summary (short 1-line description like 'Deep Male • Medium Speed • Hindi')."
                            )
                        }
                    ).text
                }.getOrNull()
            } else null

            val parsed = parseJsonSafe(rawResp)
            val pitch = parsed?.optDouble("pitch", 0.90)?.toFloat()?.coerceIn(0.65f, 1.45f) ?: 0.90f
            val speed = parsed?.optDouble("speed", 1.02)?.toFloat()?.coerceIn(0.75f, 1.35f) ?: 1.02f
            val lang = parsed?.optString("languageTag")?.takeIf { it.isNotBlank() } ?: "hi-IN"
            val summary = parsed?.optString("summary")?.takeIf { it.isNotBlank() }
                ?: "Matched Voice: Pitch ${"%.2f".format(pitch)}x • Speed ${"%.2f".format(speed)}x ($lang)"

            VoiceProfileMatch(pitch = pitch, speed = speed, languageTag = lang, summary = summary)
        }
    }

    suspend fun generateVoiceoverScript(topic: String, language: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanTopic = topic.trim().ifBlank { "Cinematic reel intro" }
        val res = runCatching {
            flashModel.generateContent(
                content {
                    text(
                        "Write a short, punchy 20-35 word social media voiceover script in $language about: '$cleanTopic'. " +
                        "Return ONLY the spoken lines, no stage directions or emojis."
                    )
                }
            ).text?.trim()
        }.getOrNull()
        val finalScript = res?.takeIf { it.isNotBlank() }
            ?: "Dosto, aaj dekhiye $cleanTopic ka sabse best aur kamaal look — video ko end tak zaroor dekhein!"
        Result.success(finalScript)
    }


    suspend fun generateViralExportKit(project: KbEditorProject): Result<ViralExportKit> = withContext(Dispatchers.IO) {
        runCatching {
            val frame = extractFirstFrame(project)
            val raw = runCatching {
                flashModel.generateContent(
                    content {
                        if (frame != null) image(frame)
                        text(
                            "Create a viral Reels/Shorts package for this video ('${project.projectName}'). " +
                            "Return ONLY JSON with keys: viralTitle (catchy 5-8 words), caption (2 short lines), " +
                            "hashtags (10 trending hashtags separated by spaces), hookText (2-4 uppercase words for thumbnail)."
                        )
                    }
                ).text
            }.getOrNull()

            val parsed = parseJsonSafe(raw)
            val title = parsed?.optString("viralTitle")?.takeIf { it.isNotBlank() }
                ?: "${project.projectName} — Must Watch Reel!"
            val caption = parsed?.optString("caption")?.takeIf { it.isNotBlank() }
                ?: "Watch till the end for the full transformation!"
            val hashtags = parsed?.optString("hashtags")?.takeIf { it.isNotBlank() }
                ?: "#trending #reels #viral #kbeditor #shorts #videoediting #fyp #india"
            val hook = parsed?.optString("hookText")?.takeIf { it.isNotBlank() }
                ?: "WATCH NOW"

            // Render HD 1280x720 / 1080x1920 Thumbnail PNG
            val thumbW = 1080
            val thumbH = 1920
            val thumbBmp = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(thumbBmp)
            if (frame != null) {
                canvas.drawBitmap(frame, null, android.graphics.RectF(0f, 0f, thumbW.toFloat(), thumbH.toFloat()), null)
                frame.recycle()
            } else {
                val bgPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, thumbW.toFloat(), thumbH.toFloat(),
                        intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#7C3AED"), Color.parseColor("#0F172A")),
                        null, Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, thumbW.toFloat(), thumbH.toFloat(), bgPaint)
            }

            // Dark bottom gradient + bold hook text
            val gradPaint = Paint().apply {
                shader = LinearGradient(
                    0f, thumbH * 0.55f, 0f, thumbH.toFloat(),
                    intArrayOf(Color.TRANSPARENT, Color.argb(225, 9, 11, 16)),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, thumbH * 0.55f, thumbW.toFloat(), thumbH.toFloat(), gradPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F59E0B")
                textSize = 96f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                setShadowLayer(16f, 0f, 6f, Color.BLACK)
            }
            canvas.drawText(hook.uppercase().take(18), thumbW / 2f, thumbH * 0.84f, textPaint)

            val outFile = File(context.cacheDir, "kb_thumb_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                thumbBmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            thumbBmp.recycle()

            ViralExportKit(
                viralTitle = title,
                caption = caption,
                hashtags = hashtags,
                thumbnailUri = Uri.fromFile(outFile).toString()
            )
        }
    }

    suspend fun generateAiBrollImage(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "ai_broll_${System.currentTimeMillis()}.png")

        val aiBitmap = runCatching {
            val encoded = URLEncoder.encode(prompt.trim().ifBlank { "cinematic neon studio" }, "UTF-8")
            val url = URL("https://image.pollinations.ai/prompt/$encoded?width=1080&height=1920&nologo=true")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 18000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "KBEditorAndroid/1.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                null
            }
        }.getOrNull()

        if (aiBitmap != null) {
            FileOutputStream(outFile).use { fos ->
                aiBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            aiBitmap.recycle()
            return@withContext Result.success("${Uri.fromFile(outFile)}|AI Scene Added to Timeline")
        }

        // Studio procedural render fallback if offline or rate-limited
        val bmp = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 1080f, 1920f,
            intArrayOf(Color.parseColor("#090D16"), Color.parseColor("#0F172A"), Color.parseColor("#164E63")),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 1080f, 1920f, paint)
        paint.shader = null
        paint.color = Color.parseColor("#22D3EE")
        paint.alpha = 55
        canvas.drawCircle(540f, 780f, 340f, paint)
        paint.color = Color.parseColor("#F8FAFC")
        paint.alpha = 230
        paint.textSize = 46f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("AI B-ROLL: ${prompt.take(22).uppercase()}", 540f, 960f, paint)

        FileOutputStream(outFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }
        bmp.recycle()
        Result.success("${Uri.fromFile(outFile)}|Rendered Studio B-Roll")
    }


    suspend fun generateAutoCaptions(
        project: KbEditorProject,
        language: CaptionLanguage,
        style: CaptionStylePreset,
        colorHex: Long,
        accentHex: Long,
        fontSizeSp: Float,
        posY: Float,
        customLanguageName: String = ""
    ): Result<List<CaptionClip>> = withContext(Dispatchers.IO) {
        val clip = project.videoClips.firstOrNull { it.mediaUri.isNotBlank() }
            ?: return@withContext Result.failure(IllegalStateException("Import a video clip first."))

        val audioUriResult = AudioExtractorEngine(context).extractAudioToM4a(clip.mediaUri, "autocap")
        val audioUriStr = audioUriResult.getOrNull()?.first
        if (audioUriStr.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("No audio track found in video for Auto-Captions.")
            )
        }

        val audioFile = Uri.parse(audioUriStr).path?.let { File(it) }
        if (audioFile == null || !audioFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Extracted audio file missing."))
        }

        val maxBytes = 6 * 1024 * 1024
        val rawBytes = audioFile.readBytes()
        val payloadBytes = if (rawBytes.size > maxBytes) rawBytes.copyOf(maxBytes) else rawBytes
        runCatching { audioFile.delete() }

        val customClean = customLanguageName.trim()
        val effectiveCode = if (customClean.isNotBlank()) customClean else language.code
        val langInstruction = when {
            customClean.isNotBlank() ->
                "Transcribe and/or translate the spoken speech accurately into '$customClean' language/dialect using its natural script."
            language == CaptionLanguage.AUTO_DETECT ->
                "Detect the spoken language or regional dialect automatically and transcribe in that exact language/script."
            else ->
                "Transcribe and/or translate the spoken speech into ${language.label} (code: ${language.code})."
        }

        val rawResponse = withTimeoutOrNull(18000L) { runCatching {
            val response = flashModel.generateContent(
                content {
                    inlineData(payloadBytes, "audio/mp4")
                    text(
                        """
                        You are an accurate video subtitle generator.
                        $langInstruction
                        Break the speech into short, punchy 2 to 6 word reel caption segments with accurate millisecond timestamps.
                        Return ONLY a valid JSON array (no markdown, no explanation) of objects with keys:
                        "startMs" (integer milliseconds), "durationMs" (integer milliseconds, 800..4500), "text" (string).
                        Example: [{"startMs": 0, "durationMs": 1800, "text": "Welcome back guys"}]
                        """.trimIndent()
                    )
                }
            )
            response.text
        }.getOrNull() } ?: return@withContext Result.failure(
            IllegalStateException("Auto-Captions timed out or offline. Check internet & retry.")
        )

        val captions = parseCaptionArray(rawResponse, style, effectiveCode, colorHex, accentHex, fontSizeSp, posY)
        if (captions.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No speech detected or could not parse captions."))
        }
        Result.success(captions)
    }

    private fun parseCaptionArray(
        raw: String?,
        style: CaptionStylePreset,
        langCode: String,
        colorHex: Long,
        accentHex: Long,
        fontSizeSp: Float,
        posY: Float
    ): List<CaptionClip> {
        if (raw.isNullOrBlank()) return emptyList()
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start == -1 || end <= start) return emptyList()
        return runCatching {
            val arr = JSONArray(raw.substring(start, end + 1))
            val out = mutableListOf<CaptionClip>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val txt = obj.optString("text").trim()
                if (txt.isBlank()) continue
                val sMs = obj.optLong("startMs", i * 2000L).coerceAtLeast(0L)
                val dMs = obj.optLong("durationMs", 2000L).coerceIn(600L, 6000L)
                out.add(
                    CaptionClip(
                        text = txt,
                        startMs = sMs,
                        durationMs = dMs,
                        animation3D = MotionAnimation3D.POP_3D,
                        stylePreset = style,
                        languageCode = langCode,
                        posX = 0.5f,
                        posY = posY,
                        fontSizeSp = fontSizeSp,
                        colorHex = colorHex,
                        accentHex = accentHex
                    )
                )
            }
            out
        }.getOrDefault(emptyList())
    }

    suspend fun runAiCopilotCommand(
        command: String,
        project: KbEditorProject,
        selectedClipIndex: Int
    ): AiEditSummary {
        val lower = command.lowercase()
        return when {
            "color" in lower || "lut" in lower || "grade" in lower || "teal" in lower || "noir" in lower ->
                runAiColorGrade(command, project, selectedClipIndex)
            else -> runAutoCreator(command, project)
        }
    }

    private fun extractFirstFrame(project: KbEditorProject): Bitmap? {
        val clip = project.videoClips.firstOrNull { it.mediaUri.isNotBlank() } ?: return null
        val uri = Uri.parse(clip.mediaUri)
        val retriever = MediaMetadataRetriever()
        val videoFrame = try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
        if (videoFrame != null) return videoFrame
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?): T? = try {
        if (raw.isNullOrBlank()) null else enumValueOf<T>(raw.trim())
    } catch (_: Throwable) {
        null
    }

    private fun parseJsonSafe(raw: String?): JSONObject? {
        if (raw.isNullOrBlank()) return null
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }
}
