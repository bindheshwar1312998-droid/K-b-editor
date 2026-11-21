package com.aistudio.kbeditor.r7v4qx.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class AudioExtractorEngine(private val context: Context) {

    suspend fun extractAudioToM4a(
        sourceVideoUri: String,
        outputTitle: String
    ): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        if (sourceVideoUri.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No video URI provided"))
        }
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return@withContext try {
            extractor.setDataSource(context, Uri.parse(sourceVideoUri), null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext Result.failure(IllegalStateException("No audio stream found in clip"))
            }

            extractor.selectTrack(audioTrackIndex)
            val safeName = outputTitle.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val outFile = File(context.cacheDir, "${safeName}_${System.currentTimeMillis()}.m4a")
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val info = MediaCodec.BufferInfo()
            var maxPtsUs = 0L

            while (true) {
                info.offset = 0
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                info.size = sampleSize
                info.presentationTimeUs = extractor.sampleTime
                if (info.presentationTimeUs > maxPtsUs) maxPtsUs = info.presentationTimeUs
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }

            Result.success(Uri.fromFile(outFile).toString() to (maxPtsUs / 1000L).coerceAtLeast(1000L))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }
}
