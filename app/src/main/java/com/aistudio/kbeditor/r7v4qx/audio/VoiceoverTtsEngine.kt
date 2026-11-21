package com.aistudio.kbeditor.r7v4qx.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

class VoiceoverTtsEngine(private val context: Context) {

    private fun readAudioDurationMs(file: File, fallbackWords: Int): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.coerceAtLeast(500L)
                ?: (fallbackWords * 420L).coerceAtLeast(1500L)
        } catch (_: Throwable) {
            (fallbackWords * 420L).coerceAtLeast(1500L)
        } finally {
            runCatching { mmr.release() }
        }
    }

    suspend fun synthesizeVoiceover(
        scriptText: String,
        languageTag: String = "en-US",
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f
    ): Result<Pair<String, Long>> = suspendCancellableCoroutine { cont ->
        val cleanText = scriptText.trim().take(3600)
        if (cleanText.isBlank()) {
            cont.resume(Result.failure(IllegalArgumentException("Voiceover script cannot be empty")))
            return@suspendCancellableCoroutine
        }

        val appCtx = context.applicationContext
        val mainHandler = Handler(Looper.getMainLooper())
        var tts: TextToSpeech? = null
        val outFile = File(appCtx.cacheDir, "kb_vo_${System.currentTimeMillis()}.wav")
        val utteranceId = "vo_${System.currentTimeMillis()}"

        fun safeShutdown() {
            mainHandler.post { runCatching { tts?.stop(); tts?.shutdown() } }
        }

        mainHandler.post {
            tts = TextToSpeech(appCtx) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    if (cont.isActive) cont.resume(Result.failure(IllegalStateException("TTS initialization failed")))
                    safeShutdown()
                    return@TextToSpeech
                }
                val desired = Locale.forLanguageTag(languageTag)
                val res = tts?.setLanguage(desired) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val hiRes = tts?.setLanguage(Locale("hi", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    if (hiRes == TextToSpeech.LANG_MISSING_DATA || hiRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale("en", "US"))
                    }
                }
                tts?.setPitch(pitch.coerceIn(0.5f, 1.8f))
                tts?.setSpeechRate(speechRate.coerceIn(0.6f, 1.6f))
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) {
                        val durMs = readAudioDurationMs(outFile, cleanText.split(Regex("\\s+")).size)
                        if (cont.isActive) {
                            cont.resume(Result.success(Uri.fromFile(outFile).toString() to durMs))
                        }
                        safeShutdown()
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException("TTS synthesis failed ($errorCode)")))
                        safeShutdown()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException("TTS synthesis failed")))
                        safeShutdown()
                    }
                })
                val synthStatus = tts?.synthesizeToFile(cleanText, null, outFile, utteranceId) ?: TextToSpeech.ERROR
                if (synthStatus != TextToSpeech.SUCCESS) {
                    if (cont.isActive) cont.resume(Result.failure(IllegalStateException("TTS synthesizeToFile returned error")))
                    safeShutdown()
                }
            }
        }

        cont.invokeOnCancellation {
            safeShutdown()
        }
    }

    suspend fun synthesizeElevenLabsVoice(
        scriptText: String,
        apiKey: String,
        voiceId: String
    ): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            val cleanVoice = voiceId.trim()
            require(cleanKey.isNotBlank() && cleanVoice.isNotBlank()) {
                "Enter both ElevenLabs API Key and Voice ID"
            }
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$cleanVoice")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("xi-api-key", cleanKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "audio/mpeg")
            }
            val payload = JSONObject().apply {
                put("text", scriptText)
                put("model_id", "eleven_multilingual_v2")
            }.toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                val errDetail = runCatching { conn.errorStream?.bufferedReader()?.readText()?.take(120) }.getOrNull().orEmpty()
                error("ElevenLabs HTTP ${conn.responseCode} $errDetail".trim())
            }
            val outFile = File(context.cacheDir, "kb_clone_${System.currentTimeMillis()}.mp3")
            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            val durMs = readAudioDurationMs(outFile, scriptText.split(Regex("\\s+")).size)
            Uri.fromFile(outFile).toString() to durMs
        }
    }

    suspend fun cloneAndSynthesizeElevenLabsVoice(
        sampleUriStr: String,
        voiceName: String,
        scriptText: String,
        apiKey: String
    ): Result<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            require(cleanKey.isNotBlank()) { "Enter ElevenLabs API Key to clone from sample" }
            require(sampleUriStr.isNotBlank()) { "Pick or record a voice sample first" }
            require(scriptText.isNotBlank()) { "Enter script text to synthesize" }

            val sampleBytes = context.contentResolver.openInputStream(Uri.parse(sampleUriStr))?.use { it.readBytes() }
                ?: error("Unable to read selected voice sample")
            require(sampleBytes.isNotEmpty()) { "Voice sample file is empty" }

            val boundary = "----KBEditorBoundary${System.currentTimeMillis()}"
            val url = URL("https://api.elevenlabs.io/v1/voices/add")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("xi-api-key", cleanKey)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            conn.outputStream.use { out ->
                val safeName = voiceName.trim().ifBlank { "KB_Clone_${System.currentTimeMillis() % 10000}" }
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"name\"\r\n\r\n$safeName\r\n".toByteArray())
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"files\"; filename=\"voice_sample.wav\"\r\n".toByteArray())
                out.write("Content-Type: audio/wav\r\n\r\n".toByteArray())
                out.write(sampleBytes)
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            if (conn.responseCode !in 200..299) {
                val errDetail = runCatching { conn.errorStream?.bufferedReader()?.readText()?.take(140) }.getOrNull().orEmpty()
                error("ElevenLabs Clone HTTP ${conn.responseCode} $errDetail".trim())
            }
            val respText = conn.inputStream.bufferedReader().use { it.readText() }
            val clonedVoiceId = JSONObject(respText).optString("voice_id")
            require(clonedVoiceId.isNotBlank()) { "ElevenLabs did not return a voice_id" }

            val (audioUri, durMs) = synthesizeElevenLabsVoice(scriptText, cleanKey, clonedVoiceId).getOrThrow()
            Triple(clonedVoiceId, audioUri, durMs)
        }
    }
}
