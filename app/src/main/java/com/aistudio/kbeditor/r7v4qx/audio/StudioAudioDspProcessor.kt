package com.aistudio.kbeditor.r7v4qx.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.aistudio.kbeditor.r7v4qx.media.NativeMediaEngine
import com.aistudio.kbeditor.r7v4qx.model.VocalIsolationMode
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Studio-Grade Real-Time Noise Cancelling & Podcast Voice Enhancer DSP Engine.
 * Runs sample-by-sample in both Live ExoPlayer Preview and Media3 MP4 Export:
 * - Sample-rate-aware 110Hz Wind & Rumble High-Pass Filter
 * - Adaptive Noise-Floor Estimator + Smooth Hold/Release Downward Expander (no word chopping)
 * - Dynamic High-Frequency Hiss Softener
 * - 3-Band Vocal Clarity EQ (280Hz Mud Cut + 2.8kHz Presence Boost + 7.5kHz Air) + Smooth Vocal Compressor
 * - Mid/Side Vocal Isolation
 */
@UnstableApi
class StudioAudioDspProcessor(
    @Volatile var windFilterEnabled: Boolean = false,
    @Volatile var noiseGateEnabled: Boolean = false,
    @Volatile var voiceEnhanceEnabled: Boolean = false,
    @Volatile var echoRemovalEnabled: Boolean = false,
    @Volatile var vocalMode: VocalIsolationMode = VocalIsolationMode.FULL_MIX
) : BaseAudioProcessor() {

    private var nativePtr: Long = if (NativeMediaEngine.isNativeAvailable) NativeMediaEngine.nativeCreateDsp() else 0L
    private var nativeScratch = ShortArray(4096)
    private var sampleRate = 48000f

    // High-pass wind/rumble filter state
    private var hpAlpha = 0.985f
    private var prevInL = 0f
    private var prevOutL = 0f
    private var prevInR = 0f
    private var prevOutR = 0f

    // Adaptive noise-floor & downward expander state
    private var envL = 0f
    private var envR = 0f
    private var noiseFloor = 0.008f
    private var gateGainL = 1f
    private var gateGainR = 1f
    private var holdSamplesL = 0
    private var holdSamplesR = 0
    private var holdDurationSamples = 8640 // ~180ms at 48kHz
    private var attackCoeff = 0.12f
    private var releaseCoeff = 0.0005f
    private var gateSmoothUp = 0.03f
    private var gateSmoothDown = 0.0012f

    // Dynamic hiss low-pass state
    private var lpL = 0f
    private var lpR = 0f

    // Voice Enhancer Biquad EQ (Mud Cut 280Hz + Presence 2.8kHz + Air 7.5kHz)
    private val mudCutL = BiquadFilter()
    private val mudCutR = BiquadFilter()
    private val presenceL = BiquadFilter()
    private val presenceR = BiquadFilter()
    private val airL = BiquadFilter()
    private val airR = BiquadFilter()

    // Smooth Vocal Compressor state
    private var compEnv = 0f
    private var deEssPrevL = 0f
    private var deEssPrevR = 0f
    private var compGain = 1f

    @Volatile var outputGain: Float = 1f
    @Volatile var outputMuted: Boolean = false
    @Volatile var noiseReductionStrength: Float = 0.75f
    @Volatile var abBypassNoiseReduction: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000).toFloat()
        if (nativePtr == 0L && NativeMediaEngine.isNativeAvailable) {
            nativePtr = NativeMediaEngine.nativeCreateDsp()
        }
        if (nativePtr != 0L) {
            NativeMediaEngine.nativeConfigureDsp(nativePtr, inputAudioFormat.sampleRate.coerceAtLeast(8000))
        }
        updateCoefficients(sampleRate)
        return inputAudioFormat
    }

    private fun updateCoefficients(sr: Float) {
        val dt = 1f / sr
        val hpRc = 1f / (2f * PI.toFloat() * 110f)
        hpAlpha = hpRc / (hpRc + dt)

        holdDurationSamples = (sr * 0.22f).toInt().coerceAtLeast(1)
        attackCoeff = 1f - exp(-1f / (sr * 0.004f))
        releaseCoeff = 1f - exp(-1f / (sr * 0.090f))
        gateSmoothUp = 1f - exp(-1f / (sr * 0.004f))
        gateSmoothDown = 1f - exp(-1f / (sr * 0.085f))

        mudCutL.configurePeaking(sr, freq = 280f, q = 1.0f, gainDb = -2.8f)
        mudCutR.configurePeaking(sr, freq = 280f, q = 1.0f, gainDb = -2.8f)
        presenceL.configurePeaking(sr, freq = 2800f, q = 1.1f, gainDb = 4.2f)
        presenceR.configurePeaking(sr, freq = 2800f, q = 1.1f, gainDb = 4.2f)
        airL.configureHighShelf(sr, freq = 7500f, gainDb = 2.2f)
        airR.configureHighShelf(sr, freq = 7500f, gainDb = 2.2f)
    }

    override fun onReset() {
        super.onReset()
        if (nativePtr != 0L) {
            NativeMediaEngine.nativeDestroyDsp(nativePtr)
            nativePtr = 0L
        }
    }

    override fun onFlush() {
        if (nativePtr != 0L) {
            NativeMediaEngine.nativeFlushDsp(nativePtr)
        }
        prevInL = 0f; prevOutL = 0f
        prevInR = 0f; prevOutR = 0f
        lpL = 0f; lpR = 0f
        envL = 0f; envR = 0f
        noiseFloor = 0.008f
        gateGainL = 1f; gateGainR = 1f
        holdSamplesL = 0; holdSamplesR = 0
        compEnv = 0f; compGain = 1f
        mudCutL.reset(); mudCutR.reset()
        presenceL.reset(); presenceR.reset()
        airL.reset(); airR.reset()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outBuffer = replaceOutputBuffer(remaining)
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val effectiveGain = if (outputMuted) 0f else outputGain.coerceIn(0f, 1.5f)
        val activeGate = noiseGateEnabled && !abBypassNoiseReduction
        val dspNeeded = windFilterEnabled || activeGate || voiceEnhanceEnabled || echoRemovalEnabled || vocalMode != VocalIsolationMode.FULL_MIX

        if (!dspNeeded && kotlin.math.abs(effectiveGain - 1f) < 0.01f) {
            outBuffer.put(inputBuffer)
            outBuffer.flip()
            return
        }

        if (nativePtr != 0L) {
            val sampleCount = remaining / 2
            if (sampleCount > 0) {
                if (nativeScratch.size < sampleCount) {
                    nativeScratch = ShortArray(sampleCount)
                }
                val shortBuf = inputBuffer.asShortBuffer()
                shortBuf.get(nativeScratch, 0, sampleCount)
                inputBuffer.position(inputBuffer.position() + sampleCount * 2)
                val vMode = when (vocalMode) {
                    VocalIsolationMode.FULL_MIX -> 0
                    VocalIsolationMode.ISOLATE_VOCALS -> 1
                    VocalIsolationMode.INSTRUMENTAL_ONLY -> 2
                }
                NativeMediaEngine.nativeSetEchoRemoval(nativePtr, echoRemovalEnabled)
                NativeMediaEngine.nativeProcessPcm16(
                    ptr = nativePtr,
                    samples = nativeScratch,
                    sampleCount = sampleCount,
                    channels = channels,
                    windFilter = windFilterEnabled,
                    noiseGate = activeGate,
                    voiceEnhance = voiceEnhanceEnabled,
                    vocalMode = vMode
                )
                if (kotlin.math.abs(effectiveGain - 1f) >= 0.01f) {
                    for (idx in 0 until sampleCount) {
                        nativeScratch[idx] = (nativeScratch[idx] * effectiveGain).toInt().coerceIn(-32767, 32767).toShort()
                    }
                }
                outBuffer.asShortBuffer().put(nativeScratch, 0, sampleCount)
                outBuffer.position(sampleCount * 2)
                outBuffer.flip()
                return
            }
        }

        if (channels >= 2) {
            while (inputBuffer.remaining() >= 4) {
                var l = inputBuffer.short / 32768f
                var r = inputBuffer.short / 32768f

                if (windFilterEnabled || noiseGateEnabled) {
                    val hpL = hpAlpha * (prevOutL + l - prevInL)
                    prevInL = l; prevOutL = hpL; l = hpL

                    val hpR = hpAlpha * (prevOutR + r - prevInR)
                    prevInR = r; prevOutR = hpR; r = hpR
                }

                if (noiseGateEnabled) {
                    val absL = abs(l)
                    val absR = abs(r)
                    envL += (if (absL > envL) attackCoeff else releaseCoeff) * (absL - envL)
                    envR += (if (absR > envR) attackCoeff else releaseCoeff) * (absR - envR)

                    // Adaptive noise floor slowly tracks quietest ambient level
                    val frameEnv = max(envL, envR)
                    if (frameEnv < noiseFloor) {
                        noiseFloor += 0.002f * (frameEnv - noiseFloor)
                    } else {
                        noiseFloor += 0.00003f * (frameEnv - noiseFloor)
                    }
                    noiseFloor = noiseFloor.coerceIn(0.002f, 0.12f)

                    val openThresh = (noiseFloor * 2.2f).coerceIn(0.010f, 0.18f)
                    val closeThresh = openThresh * 0.60f

                    if (envL >= openThresh) {
                        holdSamplesL = holdDurationSamples
                        gateGainL += gateSmoothUp * (1f - gateGainL)
                    } else if (holdSamplesL > 0) {
                        holdSamplesL--
                        gateGainL += gateSmoothUp * (1f - gateGainL)
                    } else if (envL < closeThresh) {
                        val ratio = (envL / closeThresh).coerceIn(0f, 1f)
                        val target = (0.04f + 0.96f * ratio * ratio).coerceIn(0.04f, 1f)
                        gateGainL += gateSmoothDown * (target - gateGainL)
                    }

                    if (envR >= openThresh) {
                        holdSamplesR = holdDurationSamples
                        gateGainR += gateSmoothUp * (1f - gateGainR)
                    } else if (holdSamplesR > 0) {
                        holdSamplesR--
                        gateGainR += gateSmoothUp * (1f - gateGainR)
                    } else if (envR < closeThresh) {
                        val ratio = (envR / closeThresh).coerceIn(0f, 1f)
                        val target = (0.04f + 0.96f * ratio * ratio).coerceIn(0.04f, 1f)
                        gateGainR += gateSmoothDown * (target - gateGainR)
                    }

                    l *= gateGainL
                    r *= gateGainR

                    // Dynamic hiss softener: stronger during quiet pauses, transparent during speech
                    val hissAlphaL = 0.14f + 0.80f * gateGainL
                    val hissAlphaR = 0.14f + 0.80f * gateGainR
                    lpL += hissAlphaL * (l - lpL)
                    lpR += hissAlphaR * (r - lpR)
                    val str = noiseReductionStrength.coerceIn(0f, 1f)
                    l = l * (1f - str) + lpL * str
                    r = r * (1f - str) + lpR * str
                }

                if (voiceEnhanceEnabled) {
                    val sibL = l - deEssPrevL; deEssPrevL = l
                    if (abs(sibL) > 0.22f) l -= sibL * 0.30f
                    val sibR = r - deEssPrevR; deEssPrevR = r
                    if (abs(sibR) > 0.22f) r -= sibR * 0.30f
                    l = mudCutL.process(l)
                    l = presenceL.process(l)
                    l = airL.process(l)

                    r = mudCutR.process(r)
                    r = presenceR.process(r)
                    r = airR.process(r)

                    // Smooth vocal compressor + clean makeup gain
                    val peak = max(abs(l), abs(r))
                    compEnv += (if (peak > compEnv) attackCoeff else releaseCoeff) * (peak - compEnv)
                    val targetComp = if (compEnv > 0.32f) {
                        (0.32f + (compEnv - 0.32f) / 3.0f) / compEnv
                    } else 1f
                    compGain += 0.02f * (targetComp - compGain)

                    l = softLimit(l * compGain * 1.24f)
                    r = softLimit(r * compGain * 1.24f)
                }

                val mid = (l + r) * 0.5f
                val side = (l - r) * 0.5f
                when (vocalMode) {
                    VocalIsolationMode.FULL_MIX -> Unit
                    VocalIsolationMode.ISOLATE_VOCALS -> {
                        l = (mid * 1.18f).coerceIn(-1f, 1f)
                        r = l
                    }
                    VocalIsolationMode.INSTRUMENTAL_ONLY -> {
                        l = (side * 1.4f).coerceIn(-1f, 1f)
                        r = (-side * 1.4f).coerceIn(-1f, 1f)
                    }
                }

                outBuffer.putShort(((l * effectiveGain).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                outBuffer.putShort(((r * effectiveGain).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
        } else {
            while (inputBuffer.remaining() >= 2) {
                var s = inputBuffer.short / 32768f

                if (windFilterEnabled || noiseGateEnabled) {
                    val hp = hpAlpha * (prevOutL + s - prevInL)
                    prevInL = s; prevOutL = hp; s = hp
                }

                if (noiseGateEnabled) {
                    val absS = abs(s)
                    envL += (if (absS > envL) attackCoeff else releaseCoeff) * (absS - envL)

                    if (envL < noiseFloor) {
                        noiseFloor += 0.002f * (envL - noiseFloor)
                    } else {
                        noiseFloor += 0.00003f * (envL - noiseFloor)
                    }
                    noiseFloor = noiseFloor.coerceIn(0.002f, 0.12f)

                    val openThresh = (noiseFloor * 2.2f).coerceIn(0.010f, 0.18f)
                    val closeThresh = openThresh * 0.60f

                    if (envL >= openThresh) {
                        holdSamplesL = holdDurationSamples
                        gateGainL += gateSmoothUp * (1f - gateGainL)
                    } else if (holdSamplesL > 0) {
                        holdSamplesL--
                        gateGainL += gateSmoothUp * (1f - gateGainL)
                    } else if (envL < closeThresh) {
                        val ratio = (envL / closeThresh).coerceIn(0f, 1f)
                        val target = (0.04f + 0.96f * ratio * ratio).coerceIn(0.04f, 1f)
                        gateGainL += gateSmoothDown * (target - gateGainL)
                    }

                    s *= gateGainL
                    val hissAlpha = 0.14f + 0.80f * gateGainL
                    lpL += hissAlpha * (s - lpL)
                    s = lpL
                }

                if (voiceEnhanceEnabled) {
                    s = mudCutL.process(s)
                    s = presenceL.process(s)
                    s = airL.process(s)

                    val peak = abs(s)
                    compEnv += (if (peak > compEnv) attackCoeff else releaseCoeff) * (peak - compEnv)
                    val targetComp = if (compEnv > 0.32f) {
                        (0.32f + (compEnv - 0.32f) / 3.0f) / compEnv
                    } else 1f
                    compGain += 0.02f * (targetComp - compGain)

                    s = softLimit(s * compGain * 1.24f)
                }

                // Mono has no side channel: mid/vocal is the original signal;
                // instrumental-only (side) must be silence, not unchanged audio.
                if (vocalMode == VocalIsolationMode.INSTRUMENTAL_ONLY) s = 0f
                outBuffer.putShort(((s * effectiveGain).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
        }

        outBuffer.flip()
    }

    private fun softLimit(x: Float): Float {
        val ax = abs(x)
        if (ax <= 0.85f) return x
        val sign = if (x >= 0f) 1f else -1f
        return sign * (0.85f + 0.14f * tanh(((ax - 0.85f) / 0.14f).toDouble()).toFloat())
    }

    private class BiquadFilter {
        private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
        private var a1 = 0f; private var a2 = 0f
        private var x1 = 0f; private var x2 = 0f
        private var y1 = 0f; private var y2 = 0f

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }

        fun configurePeaking(sr: Float, freq: Float, q: Float, gainDb: Float) {
            val a = 10.0.pow((gainDb / 40.0)).toFloat()
            val w0 = (2.0 * PI * min(freq, sr * 0.45f) / sr).toFloat()
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2f * q.coerceAtLeast(0.2f))
            val a0 = 1f + alpha / a
            b0 = (1f + alpha * a) / a0
            b1 = (-2f * cosW0) / a0
            b2 = (1f - alpha * a) / a0
            a1 = (-2f * cosW0) / a0
            a2 = (1f - alpha / a) / a0
        }

        fun configureHighShelf(sr: Float, freq: Float, gainDb: Float) {
            val a = 10.0.pow((gainDb / 40.0)).toFloat()
            val w0 = (2.0 * PI * min(freq, sr * 0.45f) / sr).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2f * kotlin.math.sqrt(2f)
            val twoSqrtAAlpha = 2f * kotlin.math.sqrt(a) * alpha
            val a0 = (a + 1f) - (a - 1f) * cosW0 + twoSqrtAAlpha
            b0 = (a * ((a + 1f) + (a - 1f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (-2f * a * ((a - 1f) + (a + 1f) * cosW0)) / a0
            b2 = (a * ((a + 1f) + (a - 1f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (2f * ((a - 1f) - (a + 1f) * cosW0)) / a0
            a2 = ((a + 1f) - (a - 1f) * cosW0 - twoSqrtAAlpha) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = y
            return y
        }
    }
}
