package com.aistudio.kbeditor.r7v4qx.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AudioBeatDetector(private val context: Context) {

    suspend fun detectBeatTimestampsMs(
        audioUri: String,
        fallbackDurationMs: Long = 6000L
    ): List<Long> = withContext(Dispatchers.Default) {
        if (audioUri.isBlank()) {
            return@withContext generateQuantizedBeats(fallbackDurationMs, bpm = 120)
        }

        val extractor = MediaExtractor()
        return@withContext try {
            extractor.setDataSource(context, Uri.parse(audioUri), null)
            var audioIdx = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioIdx = i
                    format = f
                    break
                }
            }
            if (audioIdx == -1 || format == null) {
                return@withContext generateQuantizedBeats(fallbackDurationMs, 120)
            }

            extractor.selectTrack(audioIdx)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext generateQuantizedBeats(fallbackDurationMs, 120)
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val windowEnergies = mutableListOf<Pair<Long, Float>>()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5000L)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 5000L)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        var sum = 0f
                        var samples = 0
                        while (outBuf.remaining() >= 2) {
                            sum += abs(outBuf.short.toInt())
                            samples++
                        }
                        if (samples > 0) {
                            windowEnergies.add((info.presentationTimeUs / 1000L) to (sum / samples))
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()

            val avgEnergy = windowEnergies.map { it.second }.average().toFloat().coerceAtLeast(1f)
            val beats = mutableListOf<Long>()
            var lastBeatMs = -400L
            for ((ts, energy) in windowEnergies) {
                if (energy > avgEnergy * 1.45f && ts - lastBeatMs >= 380L) {
                    beats.add(ts)
                    lastBeatMs = ts
                }
            }
            if (beats.isEmpty()) generateQuantizedBeats(fallbackDurationMs, 120) else beats
        } catch (_: Exception) {
            generateQuantizedBeats(fallbackDurationMs, 120)
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun generateQuantizedBeats(durationMs: Long, bpm: Int): List<Long> {
        val interval = (60_000L / bpm).coerceAtLeast(300L)
        return (0L..durationMs step interval).toList()
    }
}
