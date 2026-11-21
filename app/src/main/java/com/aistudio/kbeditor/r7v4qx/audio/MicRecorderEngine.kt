package com.aistudio.kbeditor.r7v4qx.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import java.io.File

class MicRecorderEngine(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): Result<Unit> = runCatching {
        stopSilently()
        val file = File(context.cacheDir, "kb_mic_vo_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(48000)
            setAudioEncodingBitRate(192000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        outputFile = file
        startTimeMs = System.currentTimeMillis()
    }

    fun stopRecording(): Result<Pair<String, Long>> = runCatching {
        val mr = recorder ?: error("Not recording")
        val file = outputFile ?: error("Missing output file")
        val durMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(500L)
        runCatching { mr.stop() }
        runCatching { mr.release() }
        recorder = null
        outputFile = null
        Uri.fromFile(file).toString() to durMs
    }

    fun stopSilently() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }
}
