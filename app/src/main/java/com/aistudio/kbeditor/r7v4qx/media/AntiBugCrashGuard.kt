package com.aistudio.kbeditor.r7v4qx.media

import android.content.Context
import android.os.Looper
import android.util.Log
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import java.io.File

/**
 * Ultra-Hardened Runtime Anti-Bug, OOM & Self-Healing Guard:
 * 1. Catches background-thread uncaught exceptions without crashing the UI.
 * 2. Logs any fatal/non-fatal exception to kb_last_crash.txt & Firebase Crashlytics.
 * 3. Emergency cache trimmer prevents OutOfMemoryError on low-RAM devices.
 * 4. Deep-sanitizes all VideoClips, Keyframes, ColorGrade, AudioClips, PiP & Captions.
 */
object AntiBugCrashGuard {

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appCtx = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val logFile = File(appCtx.filesDir, "kb_last_crash.txt")
                logFile.writeText("${thread.name}: ${throwable.stackTraceToString().take(4000)}")
            }
            Log.e("KBEditorGuard", "Intercepted uncaught error on ${thread.name}", throwable)
            runCatching { Firebase.crashlytics.recordException(throwable) }
            if (throwable is OutOfMemoryError) {
                trimEmergencyCache(appCtx)
            }
            if (thread.id != Looper.getMainLooper().thread.id) {
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, throwable)
        }
        trimEmergencyCache(appCtx)
    }

    fun trimEmergencyCache(context: Context) {
        runCatching {
            val rt = Runtime.getRuntime()
            val usedRatio = (rt.totalMemory() - rt.freeMemory()).toFloat() / rt.maxMemory().coerceAtLeast(1L).toFloat()
            val cutoffMs = System.currentTimeMillis() - if (usedRatio > 0.80f) 60_000L else 3_600_000L
            context.cacheDir.listFiles()?.forEach { f ->
                if (f.isFile && (f.name.startsWith("sfx_") || f.name.startsWith("kb_")) && f.lastModified() < cutoffMs) {
                    runCatching { f.delete() }
                }
            }
            if (usedRatio > 0.85f) {
                System.gc()
            }
        }
    }

    inline fun <T> safeRun(fallback: T, block: () -> T): T {
        return try {
            block()
        } catch (t: Throwable) {
            Log.w("KBEditorGuard", "Recovered from non-fatal error: ${t.message}")
            runCatching { Firebase.crashlytics.recordException(t) }
            fallback
        }
    }

    private fun safeFloat(v: Float, fallback: Float, min: Float, max: Float): Float {
        return if (v.isNaN() || v.isInfinite()) fallback else v.coerceIn(min, max)
    }

    fun sanitizeProject(project: KbEditorProject): KbEditorProject {
        val cleanClips = project.videoClips.map { clip ->
            val start = clip.trimStartMs.coerceAtLeast(0L)
            val srcDur = clip.sourceDurationMs.coerceAtLeast(300L)
            val end = clip.trimEndMs.coerceAtLeast(start + 300L)
            val cleanKfs = clip.keyframes
                .map { kf ->
                    kf.copy(
                        normalizedTime = safeFloat(kf.normalizedTime, 0f, 0f, 1f),
                        x = safeFloat(kf.x, 0.5f, 0f, 1f),
                        y = safeFloat(kf.y, 0.5f, 0f, 1f),
                        scale = safeFloat(kf.scale, 1f, 0.1f, 10f),
                        rotationDeg = safeFloat(kf.rotationDeg, 0f, -3600f, 3600f),
                        opacity = safeFloat(kf.opacity, 1f, 0f, 1f)
                    )
                }
                .sortedBy { it.normalizedTime }
            val cleanEraser = clip.magicEraserPoints.take(24).map { pt ->
                pt.copy(
                    x = safeFloat(pt.x, 0.5f, 0.01f, 0.99f),
                    y = safeFloat(pt.y, 0.5f, 0.01f, 0.99f),
                    radius = safeFloat(pt.radius, 0.045f, 0.01f, 0.2f)
                )
            }
            val g = clip.colorGrade
            val cleanGrade = g.copy(
                brightness = safeFloat(g.brightness, 0f, -1f, 1f),
                contrast = safeFloat(g.contrast, 1f, 0.2f, 3f),
                saturation = safeFloat(g.saturation, 1f, 0f, 3f),
                warmth = safeFloat(g.warmth, 0f, -1f, 1f),
                highlights = safeFloat(g.highlights, 0f, -1f, 1f),
                shadows = safeFloat(g.shadows, 0f, -1f, 1f),
                vignette = safeFloat(g.vignette, 0f, 0f, 1f),
                beautySmooth = safeFloat(g.beautySmooth, 0f, 0f, 1f),
                beautyGlow = safeFloat(g.beautyGlow, 0f, 0f, 1f),
                vibrance = safeFloat(g.vibrance, 0f, -1f, 1.5f)
            )
            clip.copy(
                sourceDurationMs = srcDur,
                trimStartMs = start,
                trimEndMs = end,
                speedMultiplier = safeFloat(clip.speedMultiplier, 1f, 0.1f, 50f),
                zoomScale = safeFloat(clip.zoomScale, 1f, 0.25f, 4f),
                panX = safeFloat(clip.panX, 0f, -1500f, 1500f),
                panY = safeFloat(clip.panY, 0f, -1500f, 1500f),
                rotationDeg = safeFloat(clip.rotationDeg, 0f, -3600f, 3600f),
                maskScale = safeFloat(clip.maskScale, 0.85f, 0.25f, 1.4f),
                maskOffsetX = safeFloat(clip.maskOffsetX, 0f, -0.45f, 0.45f),
                maskOffsetY = safeFloat(clip.maskOffsetY, 0f, -0.45f, 0.45f),
                videoFxIntensity = safeFloat(clip.videoFxIntensity, 1f, 0.1f, 10f),
                videoFxSpeed = safeFloat(clip.videoFxSpeed, 1f, 0.25f, 10f),
                animIntensity = safeFloat(clip.animIntensity, 1f, 0.2f, 10f),
                animSpeed = safeFloat(clip.animSpeed, 1f, 0.25f, 10f),
                keyframes = cleanKfs,
                magicEraserPoints = cleanEraser,
                colorGrade = cleanGrade
            )
        }
        val cleanAudio = project.audioClips.map { a ->
            a.copy(
                startMs = a.startMs.coerceAtLeast(0L),
                durationMs = a.durationMs.coerceAtLeast(50L),
                volume = safeFloat(a.volume, 1f, 0f, 2f)
            )
        }
        val cleanPip = project.pipClips.map { p ->
            p.copy(
                startMs = p.startMs.coerceAtLeast(0L),
                durationMs = p.durationMs.coerceAtLeast(100L),
                opacity = safeFloat(p.opacity, 0.85f, 0f, 1f),
                scale = safeFloat(p.scale, 0.55f, 0.1f, 3f),
                posX = safeFloat(p.posX, 0.5f, -0.5f, 1.5f),
                posY = safeFloat(p.posY, 0.5f, -0.5f, 1.5f)
            )
        }
        val cleanCaptions = project.captionClips.map { c ->
            c.copy(
                startMs = c.startMs.coerceAtLeast(0L),
                durationMs = c.durationMs.coerceAtLeast(100L),
                fontSizeSp = safeFloat(c.fontSizeSp, 22f, 8f, 120f),
                posX = safeFloat(c.posX, 0.5f, 0f, 1f),
                posY = safeFloat(c.posY, 0.78f, 0f, 1f)
            )
        }
        return project.copy(
            videoClips = cleanClips,
            audioClips = cleanAudio,
            pipClips = cleanPip,
            captionClips = cleanCaptions
        )
    }
}
