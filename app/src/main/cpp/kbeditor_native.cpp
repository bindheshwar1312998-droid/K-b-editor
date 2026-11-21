#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#ifdef HAVE_RNNOISE
extern "C" {
#include "rnnoise.h"
}
#endif

namespace {

constexpr float kPi = 3.14159265358979323846f;

struct Biquad {
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
    float a1 = 0.0f, a2 = 0.0f;
    float x1 = 0.0f, x2 = 0.0f;
    float y1 = 0.0f, y2 = 0.0f;

    void reset() { x1 = x2 = y1 = y2 = 0.0f; }

    void configurePeaking(float sr, float freq, float q, float gainDb) {
        float a = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * kPi * std::min(freq, sr * 0.45f) / sr;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) / (2.0f * std::max(q, 0.2f));
        float a0 = 1.0f + alpha / a;
        b0 = (1.0f + alpha * a) / a0;
        b1 = (-2.0f * cosW0) / a0;
        b2 = (1.0f - alpha * a) / a0;
        a1 = (-2.0f * cosW0) / a0;
        a2 = (1.0f - alpha / a) / a0;
    }

    void configureHighShelf(float sr, float freq, float gainDb) {
        float a = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * kPi * std::min(freq, sr * 0.45f) / sr;
        float cosW0 = std::cos(w0);
        float sinW0 = std::sin(w0);
        float alpha = sinW0 * 0.5f * std::sqrt(2.0f);
        float twoSqrtAAlpha = 2.0f * std::sqrt(a) * alpha;
        float a0 = (a + 1.0f) - (a - 1.0f) * cosW0 + twoSqrtAAlpha;
        b0 = (a * ((a + 1.0f) + (a - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0;
        b1 = (-2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosW0)) / a0;
        b2 = (a * ((a + 1.0f) + (a - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0;
        a1 = (2.0f * ((a - 1.0f) - (a + 1.0f) * cosW0)) / a0;
        a2 = ((a + 1.0f) - (a - 1.0f) * cosW0 - twoSqrtAAlpha) / a0;
    }

    inline float process(float x) {
        float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1; x1 = x;
        y2 = y1; y1 = y;
        return y;
    }
};

struct NativeDspState {
    float sampleRate = 48000.0f;
    float hpAlpha = 0.985f;
    float prevInL = 0.0f, prevOutL = 0.0f;
    float prevInR = 0.0f, prevOutR = 0.0f;

    float envL = 0.0f, envR = 0.0f;
    float noiseFloor = 0.008f;
    float gateGainL = 1.0f, gateGainR = 1.0f;
    int holdSamplesL = 0, holdSamplesR = 0;
    int holdDurationSamples = 8640;
    float attackCoeff = 0.12f;
    float releaseCoeff = 0.0005f;
    float gateSmoothUp = 0.03f;
    float gateSmoothDown = 0.0012f;

    float lpL = 0.0f, lpR = 0.0f;
    bool echoRemoval = false;
    float directEnvL = 0.0f, reverbEnvL = 0.0f;
    float directEnvR = 0.0f, reverbEnvR = 0.0f;

    Biquad mudCutL, mudCutR;
#ifdef HAVE_RNNOISE
    DenoiseState* rnnL = nullptr;
    DenoiseState* rnnR = nullptr;
    float rnnInL[480]{};
    float rnnOutL[480]{};
    float rnnInR[480]{};
    float rnnOutR[480]{};
    int rnnPos = 0;

    void ensureRnnoise() {
        if (!rnnL) rnnL = rnnoise_create(nullptr);
        if (!rnnR) rnnR = rnnoise_create(nullptr);
    }

    ~NativeDspState() {
        if (rnnL) { rnnoise_destroy(rnnL); rnnL = nullptr; }
        if (rnnR) { rnnoise_destroy(rnnR); rnnR = nullptr; }
    }
#endif
    Biquad presenceL, presenceR;
    Biquad airL, airR;

    float compEnv = 0.0f;
    float deEssPrevL = 0.0f, deEssPrevR = 0.0f;
    float compGain = 1.0f;

    void configure(float sr) {
        sampleRate = std::max(sr, 8000.0f);
        float dt = 1.0f / sampleRate;
        float hpRc = 1.0f / (2.0f * kPi * 110.0f);
        hpAlpha = hpRc / (hpRc + dt);

        holdDurationSamples = std::max(1, static_cast<int>(sampleRate * 0.18f));
        attackCoeff = 1.0f - std::exp(-1.0f / (sampleRate * 0.004f));
        releaseCoeff = 1.0f - std::exp(-1.0f / (sampleRate * 0.090f));
        gateSmoothUp = 1.0f - std::exp(-1.0f / (sampleRate * 0.004f));
        gateSmoothDown = 1.0f - std::exp(-1.0f / (sampleRate * 0.085f));

        mudCutL.configurePeaking(sampleRate, 280.0f, 1.0f, -2.8f);
        mudCutR.configurePeaking(sampleRate, 280.0f, 1.0f, -2.8f);
        presenceL.configurePeaking(sampleRate, 2800.0f, 1.1f, 4.2f);
        presenceR.configurePeaking(sampleRate, 2800.0f, 1.1f, 4.2f);
        airL.configureHighShelf(sampleRate, 7500.0f, 2.2f);
        airR.configureHighShelf(sampleRate, 7500.0f, 2.2f);
    }

    void flush() {
        prevInL = prevOutL = prevInR = prevOutR = 0.0f;
        lpL = lpR = envL = envR = 0.0f;
        directEnvL = 0.0f; reverbEnvL = 0.0f;
        directEnvR = 0.0f; reverbEnvR = 0.0f;
        noiseFloor = 0.008f;
        gateGainL = gateGainR = 1.0f;
        holdSamplesL = holdSamplesR = 0;
        compEnv = 0.0f;
        compGain = 1.0f;
#ifdef HAVE_RNNOISE
        rnnPos = 0;
        std::memset(rnnInL, 0, sizeof(rnnInL));
        std::memset(rnnOutL, 0, sizeof(rnnOutL));
        std::memset(rnnInR, 0, sizeof(rnnInR));
        std::memset(rnnOutR, 0, sizeof(rnnOutR));
#endif
        mudCutL.reset(); mudCutR.reset();
        presenceL.reset(); presenceR.reset();
        airL.reset(); airR.reset();
    }

    static inline float softLimit(float x) {
        float ax = std::fabs(x);
        if (ax <= 0.85f) return x;
        float sign = (x >= 0.0f) ? 1.0f : -1.0f;
        return sign * (0.85f + 0.14f * std::tanh((ax - 0.85f) / 0.14f));
    }

    static inline int16_t toInt16(float x) {
        float clamped = std::max(-1.0f, std::min(1.0f, x));
        return static_cast<int16_t>(clamped * 32767.0f);
    }
};

} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeIsRnnoiseActive(JNIEnv*, jobject) {
#ifdef HAVE_RNNOISE
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeCreateDsp(JNIEnv*, jobject) {
    auto* state = new NativeDspState();
    state->configure(48000.0f);
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT void JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeDestroyDsp(JNIEnv*, jobject, jlong ptr) {
    auto* state = reinterpret_cast<NativeDspState*>(ptr);
    delete state;
}

JNIEXPORT void JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeConfigureDsp(JNIEnv*, jobject, jlong ptr, jint sampleRate) {
    auto* state = reinterpret_cast<NativeDspState*>(ptr);
    if (state) state->configure(static_cast<float>(sampleRate));
}

JNIEXPORT void JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeSetEchoRemoval(JNIEnv*, jobject, jlong ptr, jboolean enabled) {
    auto* state = reinterpret_cast<NativeDspState*>(ptr);
    if (state) state->echoRemoval = (enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeFlushDsp(JNIEnv*, jobject, jlong ptr) {
    auto* state = reinterpret_cast<NativeDspState*>(ptr);
    if (state) state->flush();
}

JNIEXPORT void JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeProcessPcm16(
    JNIEnv* env, jobject, jlong ptr,
    jshortArray samplesArr, jint sampleCount, jint channels,
    jboolean windFilter, jboolean noiseGate, jboolean voiceEnhance, jint vocalMode
) {
    auto* state = reinterpret_cast<NativeDspState*>(ptr);
    if (!state || !samplesArr || sampleCount <= 0) return;

    jshort* samples = env->GetShortArrayElements(samplesArr, nullptr);
    if (!samples) return;

    if (channels >= 2) {
        int pairs = sampleCount / 2;
        for (int i = 0; i < pairs; ++i) {
            float l = samples[i * 2] / 32768.0f;
            float r = samples[i * 2 + 1] / 32768.0f;

            if (windFilter || noiseGate) {
                float hpL = state->hpAlpha * (state->prevOutL + l - state->prevInL);
                state->prevInL = l; state->prevOutL = hpL; l = hpL;
                float hpR = state->hpAlpha * (state->prevOutR + r - state->prevInR);
                state->prevInR = r; state->prevOutR = hpR; r = hpR;
            }

            if (noiseGate) {
#ifdef HAVE_RNNOISE
                state->ensureRnnoise();
                state->rnnInL[state->rnnPos] = l * 32767.0f;
                state->rnnInR[state->rnnPos] = r * 32767.0f;
                float rnnOutSampleL = state->rnnOutL[state->rnnPos] / 32768.0f;
                float rnnOutSampleR = state->rnnOutR[state->rnnPos] / 32768.0f;
                state->rnnPos++;
                if (state->rnnPos >= 480) {
                    if (state->rnnL) rnnoise_process_frame(state->rnnL, state->rnnOutL, state->rnnInL);
                    if (state->rnnR) rnnoise_process_frame(state->rnnR, state->rnnOutR, state->rnnInR);
                    state->rnnPos = 0;
                }
                l = rnnOutSampleL;
                r = rnnOutSampleR;
#else
                float absL = std::fabs(l);
                float absR = std::fabs(r);
                state->envL += ((absL > state->envL) ? state->attackCoeff : state->releaseCoeff) * (absL - state->envL);
                state->envR += ((absR > state->envR) ? state->attackCoeff : state->releaseCoeff) * (absR - state->envR);

                float frameEnv = std::max(state->envL, state->envR);
                if (frameEnv < state->noiseFloor) {
                    state->noiseFloor += 0.002f * (frameEnv - state->noiseFloor);
                } else {
                    state->noiseFloor += 0.00003f * (frameEnv - state->noiseFloor);
                }
                state->noiseFloor = std::max(0.002f, std::min(0.12f, state->noiseFloor));

                float openThresh = std::max(0.010f, std::min(0.18f, state->noiseFloor * 2.2f));
                float closeThresh = openThresh * 0.60f;

                if (state->envL >= openThresh) {
                    state->holdSamplesL = state->holdDurationSamples;
                    state->gateGainL += state->gateSmoothUp * (1.0f - state->gateGainL);
                } else if (state->holdSamplesL > 0) {
                    state->holdSamplesL--;
                    state->gateGainL += state->gateSmoothUp * (1.0f - state->gateGainL);
                } else if (state->envL < closeThresh) {
                    float ratio = std::max(0.0f, std::min(1.0f, state->envL / closeThresh));
                    float target = std::max(0.04f, std::min(1.0f, 0.04f + 0.96f * ratio * ratio));
                    state->gateGainL += state->gateSmoothDown * (target - state->gateGainL);
                }

                if (state->envR >= openThresh) {
                    state->holdSamplesR = state->holdDurationSamples;
                    state->gateGainR += state->gateSmoothUp * (1.0f - state->gateGainR);
                } else if (state->holdSamplesR > 0) {
                    state->holdSamplesR--;
                    state->gateGainR += state->gateSmoothUp * (1.0f - state->gateGainR);
                } else if (state->envR < closeThresh) {
                    float ratio = std::max(0.0f, std::min(1.0f, state->envR / closeThresh));
                    float target = std::max(0.04f, std::min(1.0f, 0.04f + 0.96f * ratio * ratio));
                    state->gateGainR += state->gateSmoothDown * (target - state->gateGainR);
                }

                l *= state->gateGainL;
                r *= state->gateGainR;

                float hissAlphaL = 0.14f + 0.80f * state->gateGainL;
                float hissAlphaR = 0.14f + 0.80f * state->gateGainR;
                state->lpL += hissAlphaL * (l - state->lpL);
                state->lpR += hissAlphaR * (r - state->lpR);
                l = state->lpL;
                r = state->lpR;
#endif
            }

            if (state->echoRemoval) {
                float al = std::fabs(l);
                float ar = std::fabs(r);
                state->directEnvL += ((al > state->directEnvL) ? 0.15f : 0.002f) * (al - state->directEnvL);
                state->reverbEnvL += ((al > state->reverbEnvL) ? 0.02f : 0.00025f) * (al - state->reverbEnvL);
                float tailRatioL = (state->reverbEnvL > state->directEnvL * 1.25f) ? (state->directEnvL / std::max(state->reverbEnvL, 0.001f)) : 1.0f;
                l *= std::max(0.28f, std::min(1.0f, tailRatioL));

                state->directEnvR += ((ar > state->directEnvR) ? 0.15f : 0.002f) * (ar - state->directEnvR);
                state->reverbEnvR += ((ar > state->reverbEnvR) ? 0.02f : 0.00025f) * (ar - state->reverbEnvR);
                float tailRatioR = (state->reverbEnvR > state->directEnvR * 1.25f) ? (state->directEnvR / std::max(state->reverbEnvR, 0.001f)) : 1.0f;
                r *= std::max(0.28f, std::min(1.0f, tailRatioR));
            }

            if (voiceEnhance) {
                float sibL = l - state->deEssPrevL; state->deEssPrevL = l;
                if (std::fabs(sibL) > 0.22f) l -= sibL * 0.30f;
                float sibR = r - state->deEssPrevR; state->deEssPrevR = r;
                if (std::fabs(sibR) > 0.22f) r -= sibR * 0.30f;
                l = state->airL.process(state->presenceL.process(state->mudCutL.process(l)));
                r = state->airR.process(state->presenceR.process(state->mudCutR.process(r)));

                float peak = std::max(std::fabs(l), std::fabs(r));
                state->compEnv += ((peak > state->compEnv) ? state->attackCoeff : state->releaseCoeff) * (peak - state->compEnv);
                float targetComp = (state->compEnv > 0.32f)
                    ? (0.32f + (state->compEnv - 0.32f) / 3.0f) / state->compEnv
                    : 1.0f;
                state->compGain += 0.02f * (targetComp - state->compGain);

                l = NativeDspState::softLimit(l * state->compGain * 1.24f);
                r = NativeDspState::softLimit(r * state->compGain * 1.24f);
            }

            if (vocalMode == 1) {
                float mid = (l + r) * 0.5f * 1.18f;
                l = mid;
                r = mid;
            } else if (vocalMode == 2) {
                float side = (l - r) * 0.5f * 1.4f;
                l = side;
                r = -side;
            }

            samples[i * 2] = NativeDspState::toInt16(l);
            samples[i * 2 + 1] = NativeDspState::toInt16(r);
        }
    } else {
        for (int i = 0; i < sampleCount; ++i) {
            float s = samples[i] / 32768.0f;
            if (windFilter || noiseGate) {
                float hp = state->hpAlpha * (state->prevOutL + s - state->prevInL);
                state->prevInL = s; state->prevOutL = hp; s = hp;
            }
            if (noiseGate) {
                float absS = std::fabs(s);
                state->envL += ((absS > state->envL) ? state->attackCoeff : state->releaseCoeff) * (absS - state->envL);
                if (state->envL < state->noiseFloor) {
                    state->noiseFloor += 0.002f * (state->envL - state->noiseFloor);
                } else {
                    state->noiseFloor += 0.00003f * (state->envL - state->noiseFloor);
                }
                state->noiseFloor = std::max(0.002f, std::min(0.12f, state->noiseFloor));
                float openThresh = std::max(0.010f, std::min(0.18f, state->noiseFloor * 2.2f));
                float closeThresh = openThresh * 0.60f;
                if (state->envL >= openThresh) {
                    state->holdSamplesL = state->holdDurationSamples;
                    state->gateGainL += state->gateSmoothUp * (1.0f - state->gateGainL);
                } else if (state->holdSamplesL > 0) {
                    state->holdSamplesL--;
                    state->gateGainL += state->gateSmoothUp * (1.0f - state->gateGainL);
                } else if (state->envL < closeThresh) {
                    float ratio = std::max(0.0f, std::min(1.0f, state->envL / closeThresh));
                    float target = std::max(0.04f, std::min(1.0f, 0.04f + 0.96f * ratio * ratio));
                    state->gateGainL += state->gateSmoothDown * (target - state->gateGainL);
                }
                s *= state->gateGainL;
                float hissAlpha = 0.14f + 0.80f * state->gateGainL;
                state->lpL += hissAlpha * (s - state->lpL);
                s = state->lpL;
            }
            if (voiceEnhance) {
                s = state->airL.process(state->presenceL.process(state->mudCutL.process(s)));
                float peak = std::fabs(s);
                state->compEnv += ((peak > state->compEnv) ? state->attackCoeff : state->releaseCoeff) * (peak - state->compEnv);
                float targetComp = (state->compEnv > 0.32f)
                    ? (0.32f + (state->compEnv - 0.32f) / 3.0f) / state->compEnv
                    : 1.0f;
                state->compGain += 0.02f * (targetComp - state->compGain);
                s = NativeDspState::softLimit(s * state->compGain * 1.24f);
            }
            // A mono stream has no side channel to extract.
            if (vocalMode == 2) s = 0.0f;
            samples[i] = NativeDspState::toInt16(s);
        }
    }

    env->ReleaseShortArrayElements(samplesArr, samples, 0);
}

JNIEXPORT jboolean JNICALL
Java_com_aistudio_kbeditor_r7v4qx_media_NativeMediaEngine_nativeEnhanceBitmap(
    JNIEnv* env, jobject, jobject bitmap, jfloat contrast, jfloat saturation, jfloat brightness
) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0 || !pixels) return JNI_FALSE;

    auto* data = static_cast<uint8_t*>(pixels);
    const float bShift = brightness * 255.0f;

    // Android bitmap rows may contain padding; use the reported row stride
    // instead of assuming tightly packed width * 4 bytes.
    for (uint32_t y = 0; y < info.height; ++y) {
      uint8_t* row = data + static_cast<size_t>(y) * info.stride;
      for (uint32_t x = 0; x < info.width; ++x) {
        uint8_t* px = row + static_cast<size_t>(x) * 4;
        float r = static_cast<float>(px[0]);
        float g = static_cast<float>(px[1]);
        float b = static_cast<float>(px[2]);

        float lumaIn = 0.299f * r + 0.587f * g + 0.114f * b;
        float shadowLift = (lumaIn < 90.0f) ? (90.0f - lumaIn) * 0.14f : 0.0f;

        r = (r + shadowLift - 128.0f) * contrast + 128.0f + bShift;
        g = (g + shadowLift - 128.0f) * contrast + 128.0f + bShift;
        b = (b + shadowLift - 128.0f) * contrast + 128.0f + bShift;

        float luma = 0.299f * r + 0.587f * g + 0.114f * b;
        r = luma + (r - luma) * saturation;
        g = luma + (g - luma) * saturation;
        b = luma + (b - luma) * saturation;

        px[0] = static_cast<uint8_t>(std::max(0.0f, std::min(255.0f, r)));
        px[1] = static_cast<uint8_t>(std::max(0.0f, std::min(255.0f, g)));
        px[2] = static_cast<uint8_t>(std::max(0.0f, std::min(255.0f, b)));
      }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

} // extern "C"
