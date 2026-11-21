package com.aistudio.kbeditor.r7v4qx.media

import android.graphics.Bitmap

/**
 * High-speed C++ NDK Engine Bridge (kbeditor_native.so).
 * Safely falls back to Kotlin/JVM when native library is unavailable (e.g. unit tests).
 */
object NativeMediaEngine {

    val isNativeAvailable: Boolean = runCatching {
        System.loadLibrary("kbeditor_native")
        true
    }.getOrDefault(false)

    external fun nativeIsRnnoiseActive(): Boolean
    external fun nativeCreateDsp(): Long
    external fun nativeDestroyDsp(ptr: Long)
    external fun nativeConfigureDsp(ptr: Long, sampleRate: Int)
    external fun nativeFlushDsp(ptr: Long)
    external fun nativeSetEchoRemoval(ptr: Long, enabled: Boolean)
    external fun nativeProcessPcm16(
        ptr: Long,
        samples: ShortArray,
        sampleCount: Int,
        channels: Int,
        windFilter: Boolean,
        noiseGate: Boolean,
        voiceEnhance: Boolean,
        vocalMode: Int
    )

    external fun nativeEnhanceBitmap(
        bitmap: Bitmap,
        contrast: Float,
        saturation: Float,
        brightness: Float
    ): Boolean
}
