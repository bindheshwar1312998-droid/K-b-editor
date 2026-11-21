package com.aistudio.kbeditor.r7v4qx

import com.aistudio.kbeditor.r7v4qx.media.AntiBugCrashGuard
import com.aistudio.kbeditor.r7v4qx.model.EffectCatalog
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import com.aistudio.kbeditor.r7v4qx.model.KeyframePoint
import com.aistudio.kbeditor.r7v4qx.model.VideoClip
import com.aistudio.kbeditor.r7v4qx.vfx.CurveEvaluators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KbEditorEngineUnitTest {

    @Test
    fun antiBugSanitizerHealsNaNAndInvalidTrim() {
        val broken = KbEditorProject(
            videoClips = listOf(
                VideoClip(
                    title = "Broken",
                    trimStartMs = 2000L,
                    trimEndMs = 500L,
                    zoomScale = Float.NaN,
                    speedMultiplier = 0.01f,
                    maskOffsetX = 9.9f
                )
            )
        )
        val healed = AntiBugCrashGuard.sanitizeProject(broken)
        val clip = healed.videoClips.first()
        assertTrue(clip.trimEndMs >= clip.trimStartMs + 300L)
        assertEquals(1.0f, clip.zoomScale, 0.001f)
        assertTrue(clip.speedMultiplier >= 0.1f)
        assertTrue(clip.maskOffsetX <= 0.45f)
    }

    @Test
    fun keyframeCurvesInterpolateProperly() {
        val kfs = listOf(
            KeyframePoint(normalizedTime = 0f, scale = 1.0f, easingCurve = "Linear"),
            KeyframePoint(normalizedTime = 1f, scale = 2.0f, easingCurve = "Linear")
        )
        val midLinear = CurveEvaluators.interpolateKeyframes(kfs, 0.5f)
        assertEquals(1.5f, midLinear.scale, 0.01f)

        val easeOutKfs = listOf(
            KeyframePoint(normalizedTime = 0f, scale = 1.0f, easingCurve = "Ease Out"),
            KeyframePoint(normalizedTime = 1f, scale = 2.0f, easingCurve = "Ease Out")
        )
        val midEaseOut = CurveEvaluators.interpolateKeyframes(easeOutKfs, 0.5f)
        assertTrue(midEaseOut.scale > 1.7f)
    }

    @Test
    fun effectAndAnimationCatalogsLoaded() {
        assertTrue(EffectCatalog.allVideoFx.size >= 7000)
        assertTrue(EffectCatalog.allAnimations.size >= 3000)
    }

    @Test
    fun projectTotalDurationCalculatesWithSpeed() {
        val proj = KbEditorProject(
            videoClips = listOf(
                VideoClip(trimStartMs = 0L, trimEndMs = 4000L, speedMultiplier = 2.0f),
                VideoClip(trimStartMs = 0L, trimEndMs = 3000L, speedMultiplier = 1.0f)
            )
        )
        assertEquals(5000L, proj.totalDurationMs)
    }
}
