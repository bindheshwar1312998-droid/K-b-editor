package com.aistudio.kbeditor.r7v4qx.audio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class CapCutSfxPreset(val label: String, val category: String, val durationMs: Int) {
    WHOOSH_FAST("Whoosh", "Transition", 380),
    SWOOSH_DEEP("Deep Swoosh", "Transition", 520),
    GLITCH_ZAP("Glitch Zap", "Transition", 240),
    RISER_BUILD("Riser Swell", "Transition", 1100),
    BASS_DROP("Bass Drop", "Impact", 680),
    CINEMA_BOOM("Cinema Boom", "Impact", 850),
    POP_BUBBLE("Text Pop", "UI & Text", 120),
    SHUTTER_CLICK("Camera Snap", "UI & Text", 140),
    DING_BELL("Ding!", "UI & Text", 420),
    COIN_CHIME("Coin Win", "UI & Text", 460),
    SPARKLE_MAGIC("Magic Sparkle", "Vibe", 560),
    CENSOR_BEEP("Beep", "Comedy", 350),
    PUNCH_HIT("Action Punch", "Action", 260),
    SWORD_SLASH("Sword Slash", "Action", 340),
    LASER_PEW("Laser Zap", "Action", 280),
    EXPLOSION_BOOM("Explosion", "Action", 900),
    GAVEL_SLAM("Gavel Slam", "Auction", 320),
    COUNTDOWN_TICK("Timer Tick", "Auction", 180),
    CROWD_CHEER("Crowd Cheer", "Auction", 1100),
    APPLAUSE_CLAP("Applause", "Auction", 950),
    DRUM_ROLL("Drum Roll", "Impact", 1000),
    RECORD_SCRATCH("Scratch", "Transition", 360)
}

data class SfxCatalogItem(
    val name: String,
    val preset: CapCutSfxPreset,
    val pitchMul: Float = 1.0f,
    val gainMul: Float = 1.0f
)

object SfxCatalog {
    val allSfx: List<SfxCatalogItem> by lazy {
        val list = mutableListOf<SfxCatalogItem>()
        CapCutSfxPreset.entries.forEach { p ->
            list.add(SfxCatalogItem(p.label, p, 1.0f, 1.0f))
        }
        val variants = listOf(
            "Deep" to (0.68f to 1.15f), "Sub" to (0.55f to 1.2f), "Heavy" to (0.75f to 1.25f),
            "Crisp" to (1.25f to 1.05f), "Bright" to (1.40f to 1.0f), "Fast" to (1.55f to 1.0f),
            "Ultra Fast" to (1.85f to 1.0f), "Slow" to (0.72f to 1.0f), "Punchy" to (0.92f to 1.3f),
            "Soft" to (0.95f to 0.65f), "Mega" to (0.80f to 1.35f), "Turbo" to (1.65f to 1.15f),
            "High" to (1.50f to 0.95f), "Low" to (0.62f to 1.1f), "Cinema" to (0.78f to 1.2f),
            "Vlog" to (1.12f to 0.9f), "Reels" to (1.18f to 1.1f), "Shorts" to (1.30f to 1.1f),
            "TikTok" to (1.22f to 1.15f), "Action" to (0.88f to 1.3f), "Retro" to (0.82f to 0.95f),
            "Cyber" to (1.45f to 1.1f), "Neon" to (1.35f to 1.05f), "Mini" to (1.70f to 0.8f),
            "Max" to (0.90f to 1.4f), "Studio" to (1.05f to 1.1f), "Pro" to (1.08f to 1.15f),
            "Lite" to (1.15f to 0.7f), "Hard" to (0.85f to 1.35f), "Sharp" to (1.42f to 1.1f),
            "Airy" to (1.28f to 0.75f), "Warm" to (0.84f to 1.0f), "Dark" to (0.60f to 1.15f),
            "Snap" to (1.60f to 1.2f), "Drop" to (0.70f to 1.3f), "Pop" to (1.32f to 1.1f),
            "Zap" to (1.52f to 1.15f), "Boom" to (0.58f to 1.35f), "Spark" to (1.48f to 0.95f),
            "Glow" to (1.10f to 0.85f), "Whip" to (1.38f to 1.2f), "Slash" to (1.44f to 1.15f),
            "Impact" to (0.76f to 1.35f), "Boost" to (1.02f to 1.35f), "Echo" to (0.94f to 0.9f),
            "Epic" to (0.66f to 1.3f), "Clean" to (1.04f to 0.95f), "Wild" to (1.75f to 1.2f),
            "Hyper" to (1.90f to 1.15f)
        )
        for (p in CapCutSfxPreset.entries) {
            for ((vName, pg) in variants) {
                list.add(SfxCatalogItem("${p.label} $vName", p, pg.first, pg.second))
            }
        }
        list
    }
}

class StudioSfxEngine(private val context: Context) {

    suspend fun generateSfxWav(preset: CapCutSfxPreset, pitchMul: Float = 1.0f, gainMul: Float = 1.0f, customLabel: String = preset.label): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        runCatching {
            val sampleRate = 44100
            val durScaled = (preset.durationMs / pitchMul.coerceIn(0.5f, 2.0f)).toInt().coerceIn(80, 2200)
            val numSamples = (sampleRate * durScaled) / 1000
            val pcm = ShortArray(numSamples)
            val rng = Random(preset.ordinal * 1337)
            var lp = 0.0

            for (i in 0 until numSamples) {
                val t = (i.toDouble() / sampleRate) * pitchMul.coerceIn(0.5f, 2.0f)
                val norm = i.toDouble() / numSamples.coerceAtLeast(1)

                val sample = when (preset) {
                    CapCutSfxPreset.WHOOSH_FAST -> {
                        val env = sin(PI * norm) * sin(PI * norm)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        val alpha = 0.05 + 0.35 * sin(PI * norm)
                        lp += alpha * (noise - lp)
                        lp * env * 0.9
                    }
                    CapCutSfxPreset.SWOOSH_DEEP -> {
                        val env = sin(PI * norm)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lp += 0.08 * (noise - lp)
                        val sub = sin(2.0 * PI * (120.0 - 70.0 * norm) * t) * 0.4
                        (lp * 0.75 + sub) * env
                    }
                    CapCutSfxPreset.GLITCH_ZAP -> {
                        val env = exp(-6.0 * norm)
                        val freq = 320.0 + 900.0 * sin(2.0 * PI * 32.0 * t)
                        val sq = if (sin(2.0 * PI * freq * t) >= 0) 0.6 else -0.6
                        (sq + rng.nextDouble(-0.3, 0.3)) * env
                    }
                    CapCutSfxPreset.RISER_BUILD -> {
                        val env = norm * norm
                        val freq = 140.0 + 720.0 * norm * norm
                        val tone = sin(2.0 * PI * freq * t) * 0.55
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lp += (0.04 + 0.3 * norm) * (noise - lp)
                        (tone + lp * 0.5) * env
                    }
                    CapCutSfxPreset.BASS_DROP -> {
                        val env = exp(-4.2 * norm)
                        val freq = 135.0 * exp(-3.5 * norm) + 38.0
                        sin(2.0 * PI * freq * t) * env * 0.95
                    }
                    CapCutSfxPreset.CINEMA_BOOM -> {
                        val env = exp(-3.0 * norm)
                        val sub = sin(2.0 * PI * (95.0 - 55.0 * norm) * t)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lp += 0.06 * (noise - lp)
                        (sub * 0.7 + lp * 0.5) * env
                    }
                    CapCutSfxPreset.POP_BUBBLE -> {
                        val env = exp(-12.0 * norm)
                        val freq = 920.0 - 640.0 * norm
                        sin(2.0 * PI * freq * t) * env * 0.9
                    }
                    CapCutSfxPreset.SHUTTER_CLICK -> {
                        val burst1 = if (norm < 0.18) rng.nextDouble(-1.0, 1.0) * (1.0 - norm / 0.18) else 0.0
                        val burst2 = if (norm in 0.45..0.68) rng.nextDouble(-1.0, 1.0) * (1.0 - (norm - 0.45) / 0.23) else 0.0
                        (burst1 + burst2) * 0.85
                    }
                    CapCutSfxPreset.DING_BELL -> {
                        val env = exp(-5.5 * norm)
                        (sin(2.0 * PI * 1046.5 * t) * 0.7 + sin(2.0 * PI * 2093.0 * t) * 0.3) * env
                    }
                    CapCutSfxPreset.COIN_CHIME -> {
                        val env = exp(-4.8 * norm)
                        val f = if (norm < 0.26) 987.77 else 1318.51
                        sin(2.0 * PI * f * t) * env * 0.85
                    }
                    CapCutSfxPreset.SPARKLE_MAGIC -> {
                        val env = exp(-3.5 * norm)
                        val step = (norm * 4).toInt().coerceIn(0, 3)
                        val freqs = doubleArrayOf(880.0, 1108.73, 1318.51, 1760.0)
                        (sin(2.0 * PI * freqs[step] * t) * 0.65 + sin(2.0 * PI * freqs[step] * 2.0 * t) * 0.25) * env
                    }
                    CapCutSfxPreset.CENSOR_BEEP -> {
                        val fade = if (norm < 0.05) norm / 0.05 else if (norm > 0.92) (1.0 - norm) / 0.08 else 1.0
                        sin(2.0 * PI * 1000.0 * t) * fade * 0.75
                    }
                    CapCutSfxPreset.PUNCH_HIT -> {
                        val env = exp(-9.0 * norm)
                        val thump = sin(2.0 * PI * (180.0 - 130.0 * norm) * t) * 0.8
                        val snap = if (norm < 0.15) rng.nextDouble(-1.0, 1.0) * 0.6 else 0.0
                        (thump + snap) * env
                    }
                    CapCutSfxPreset.SWORD_SLASH -> {
                        val env = exp(-6.5 * norm)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lp += 0.35 * (noise - lp)
                        val ring = sin(2.0 * PI * (2400.0 - 1100.0 * norm) * t) * 0.35
                        (lp * 0.8 + ring) * env
                    }
                    CapCutSfxPreset.LASER_PEW -> {
                        val env = exp(-7.0 * norm)
                        val freq = 1600.0 * exp(-4.5 * norm) + 180.0
                        sin(2.0 * PI * freq * t) * env * 0.85
                    }
                    CapCutSfxPreset.EXPLOSION_BOOM -> {
                        val env = exp(-3.2 * norm)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lp += (0.04 + 0.12 * (1.0 - norm)) * (noise - lp)
                        val sub = sin(2.0 * PI * (75.0 - 40.0 * norm) * t) * 0.65
                        (lp * 0.9 + sub) * env
                    }
                    CapCutSfxPreset.GAVEL_SLAM -> {
                        val hit1 = if (norm < 0.45) exp(-14.0 * norm) * sin(2.0 * PI * (220.0 - 120.0 * norm) * t) else 0.0
                        val hit2 = if (norm >= 0.45) exp(-14.0 * (norm - 0.45)) * sin(2.0 * PI * 190.0 * t) else 0.0
                        (hit1 + hit2) * 0.9
                    }
                    CapCutSfxPreset.COUNTDOWN_TICK -> {
                        val env = exp(-16.0 * norm)
                        (sin(2.0 * PI * 1450.0 * t) * 0.7 + rng.nextDouble(-0.2, 0.2)) * env
                    }
                    CapCutSfxPreset.CROWD_CHEER -> {
                        val env = sin(PI * norm)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        lp += 0.18 * (noise - lp)
                        val harmonic = sin(2.0 * PI * 320.0 * t) * 0.15 + sin(2.0 * PI * 480.0 * t) * 0.12
                        (lp * 0.8 + harmonic) * env * 0.85
                    }
                    CapCutSfxPreset.APPLAUSE_CLAP -> {
                        val env = sin(PI * norm)
                        val burst = if ((i / 650) % 2 == 0) rng.nextDouble(-1.0, 1.0) else rng.nextDouble(-0.3, 0.3)
                        lp += 0.25 * (burst - lp)
                        lp * env * 0.85
                    }
                    CapCutSfxPreset.DRUM_ROLL -> {
                        val beat = (norm * 14.0) % 1.0
                        val env = exp(-6.0 * beat) * (0.4 + 0.6 * norm)
                        val snare = rng.nextDouble(-1.0, 1.0) * 0.6 + sin(2.0 * PI * 185.0 * t) * 0.4
                        snare * env
                    }
                    CapCutSfxPreset.RECORD_SCRATCH -> {
                        val env = sin(PI * norm)
                        val freq = 350.0 + 450.0 * sin(2.0 * PI * 11.0 * t)
                        sin(2.0 * PI * freq * t) * env * 0.8
                    }
                }
                pcm[i] = ((sample * gainMul).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }

            val outFile = File(context.cacheDir, "sfx_${preset.name.lowercase()}_${System.currentTimeMillis()}.wav")
            writeWavFile(outFile, pcm, sampleRate)
            Uri.fromFile(outFile).toString() to durScaled.toLong()
        }
    }

    private fun writeWavFile(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1.toShort()) // PCM
            putShort(1.toShort()) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray())
            putInt(dataSize)
        }.array()

        val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) pcmBytes.putShort(s)

        FileOutputStream(file).use { fos ->
            fos.write(header)
            fos.write(pcmBytes.array())
        }
    }
}
