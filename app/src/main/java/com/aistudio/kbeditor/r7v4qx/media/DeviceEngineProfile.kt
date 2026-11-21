package com.aistudio.kbeditor.r7v4qx.media

import android.os.Build

enum class ChipsetFamily(val label: String) {
    SNAPDRAGON_ADRENO("Snapdragon / Adreno GPU Engine"),
    MEDIATEK_MALI("MediaTek / Mali GPU Engine"),
    BALANCED_GENERIC("Balanced Universal GPU Engine")
}

data class DeviceEngineTuning(
    val chipset: ChipsetFamily,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int,
    val bitrate4k: Int,
    val bitrate1080p: Int,
    val bitrate720p: Int,
    val preferHevcOn4k: Boolean
)

object DeviceEngineProfile {

    val activeTuning: DeviceEngineTuning by lazy {
        val socManuf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.orEmpty()
        } else {
            ""
        }
        val fingerprint = listOf(
            socManuf,
            Build.HARDWARE.orEmpty(),
            Build.BOARD.orEmpty(),
            Build.BRAND.orEmpty(),
            Build.MODEL.orEmpty()
        ).joinToString(" ").lowercase()

        when {
            "qcom" in fingerprint || "qualcomm" in fingerprint || "snapdragon" in fingerprint || "adreno" in fingerprint || "sm" in Build.BOARD.lowercase() -> {
                DeviceEngineTuning(
                    chipset = ChipsetFamily.SNAPDRAGON_ADRENO,
                    minBufferMs = 1500,
                    maxBufferMs = 8000,
                    playbackBufferMs = 250,
                    rebufferMs = 500,
                    bitrate4k = 35_000_000,
                    bitrate1080p = 22_000_000,
                    bitrate720p = 10_000_000,
                    preferHevcOn4k = true
                )
            }
            "mt" in fingerprint || "mediatek" in fingerprint || "helio" in fingerprint || "dimensity" in fingerprint || "mali" in fingerprint -> {
                DeviceEngineTuning(
                    chipset = ChipsetFamily.MEDIATEK_MALI,
                    minBufferMs = 2000,
                    maxBufferMs = 9000,
                    playbackBufferMs = 350,
                    rebufferMs = 650,
                    bitrate4k = 28_000_000,
                    bitrate1080p = 18_000_000,
                    bitrate720p = 9_000_000,
                    preferHevcOn4k = false
                )
            }
            else -> {
                DeviceEngineTuning(
                    chipset = ChipsetFamily.BALANCED_GENERIC,
                    minBufferMs = 1800,
                    maxBufferMs = 8500,
                    playbackBufferMs = 300,
                    rebufferMs = 600,
                    bitrate4k = 30_000_000,
                    bitrate1080p = 20_000_000,
                    bitrate720p = 9_500_000,
                    preferHevcOn4k = false
                )
            }
        }
    }
}
