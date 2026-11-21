package com.aistudio.kbeditor.r7v4qx.vfx

import androidx.compose.ui.graphics.ColorMatrix
import com.aistudio.kbeditor.r7v4qx.model.ColorGradeSpec
import com.aistudio.kbeditor.r7v4qx.model.KeyframePoint
import com.aistudio.kbeditor.r7v4qx.model.SpeedRampPreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object CurveEvaluators {

    fun evaluateSpeedMultiplier(preset: SpeedRampPreset, normalizedTime: Float): Float {
        val t = normalizedTime.coerceIn(0f, 1f)
        return when (preset) {
            SpeedRampPreset.CONSTANT -> 1.0f
            SpeedRampPreset.HERO_DIP -> {
                val dip = sin(t * PI).toFloat()
                (2.2f - 1.75f * dip).coerceIn(0.35f, 2.5f)
            }
            SpeedRampPreset.FLASH_IN -> {
                if (t < 0.25f) 3.0f - (t / 0.25f) * 2.0f else 1.0f
            }
            SpeedRampPreset.BULLET_TIME -> {
                if (t in 0.3f..0.7f) 0.35f else 2.0f
            }
            SpeedRampPreset.BUILD_UP -> {
                0.6f + 2.2f * t.pow(1.8f)
            }
        }
    }

    fun interpolateKeyframes(
        keyframes: List<KeyframePoint>,
        normalizedTime: Float
    ): KeyframePoint {
        if (keyframes.isEmpty()) return KeyframePoint(normalizedTime = normalizedTime)
        val sorted = keyframes.sortedBy { it.normalizedTime }
        val t = normalizedTime.coerceIn(0f, 1f)
        if (t <= sorted.first().normalizedTime) return sorted.first()
        if (t >= sorted.last().normalizedTime) return sorted.last()

        for (i in 0 until sorted.lastIndex) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (t in a.normalizedTime..b.normalizedTime) {
                val span = (b.normalizedTime - a.normalizedTime).coerceAtLeast(0.0001f)
                val localT = ((t - a.normalizedTime) / span).coerceIn(0f, 1f)
                val eased = when (a.easingCurve) {
                    "Linear" -> localT
                    "Ease In" -> localT * localT * localT
                    "Ease Out" -> 1f - (1f - localT).pow(3)
                    "Custom Bezier" -> {
                        val u = 1f - localT
                        // Alight Motion style snappy S-curve cubic bezier (P1Y=0.05, P2Y=0.95)
                        (3f * u * u * localT * 0.05f + 3f * u * localT * localT * 0.95f + localT * localT * localT).coerceIn(0f, 1f)
                    }
                    "Bounce" -> {
                        val s = sin(localT * PI.toFloat() * 1.5f)
                        (localT + s * 0.16f * (1f - localT)).coerceIn(0f, 1.15f)
                    }
                    else -> (1f - cos(localT * PI.toFloat())) * 0.5f
                }
                return KeyframePoint(
                    normalizedTime = t,
                    x = lerp(a.x, b.x, eased),
                    y = lerp(a.y, b.y, eased),
                    scale = lerp(a.scale, b.scale, eased),
                    rotationDeg = lerp(a.rotationDeg, b.rotationDeg, eased),
                    easingCurve = a.easingCurve,
                    opacity = lerp(a.opacity, b.opacity, eased)
                )
            }
        }
        return sorted.last()
    }

    fun buildGradeColorMatrix(grade: ColorGradeSpec): ColorMatrix {
        val contrast = grade.contrast.coerceIn(0.5f, 2.0f)
        val sat = (grade.saturation + grade.vibrance * 0.45f).coerceIn(0f, 2.5f)
        val brightnessShift = (grade.brightness + grade.lift.luma * 0.25f + grade.gamma.luma * 0.2f + grade.gain.luma * 0.2f) * 255f

        // 3-Way Wheel RGB offsets combined with warmth
        val rOffset = (grade.lift.dx * 0.35f + grade.gamma.dx * 0.45f + grade.gain.dx * 0.5f + grade.warmth * 0.25f) * 60f
        val gOffset = (grade.lift.dy * 0.25f + grade.gamma.dy * 0.3f + grade.gain.dy * 0.35f) * 45f
        val bOffset = (-grade.lift.dx * 0.35f - grade.gamma.dx * 0.45f - grade.gain.dx * 0.5f - grade.warmth * 0.25f) * 60f

        val highlightGain = 1f + grade.highlights * 0.42f
        val shadowLift = grade.shadows * 68f
        val translate = 128f * (1f - contrast) + brightnessShift + shadowLift

        val sr = (1f - sat) * 0.2126f
        val sg = (1f - sat) * 0.7152f
        val sb = (1f - sat) * 0.0722f

        val scaleR = contrast * highlightGain
        val scaleG = contrast * highlightGain
        val scaleB = contrast * highlightGain

        return ColorMatrix(
            floatArrayOf(
                (sr + sat) * scaleR, sg * scaleR, sb * scaleR, 0f, translate + rOffset,
                sr * scaleG, (sg + sat) * scaleG, sb * scaleG, 0f, translate + gOffset,
                sr * scaleB, sg * scaleB, (sb + sat) * scaleB, 0f, translate + bOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
