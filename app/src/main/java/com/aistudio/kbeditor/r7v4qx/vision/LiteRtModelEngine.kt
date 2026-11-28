package com.aistudio.kbeditor.r7v4qx.vision

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * On-Device LiteRT (.tflite) AI Model Runner:
 * - Loads bundled .tflite models from assets/models/<name>.tflite
 *   OR downloaded open-source Hugging Face / Kaggle .tflite models from filesDir/models/
 * - Runs RGB image-to-image / enhancement inference cleanly on-device with 4 threads.
 * - Safe fallback: returns null if model file isn't present so C++ NDK engine handles it seamlessly.
 */
class LiteRtModelEngine(private val context: Context) {

    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply { mkdirs() }
    }

    fun hasModel(modelFileName: String): Boolean {
        if (File(modelsDir, modelFileName).exists()) return true
        return runCatching {
            context.assets.open("models/$modelFileName").use { true }
        }.getOrDefault(false)
    }

    suspend fun downloadModelFromUrl(
        modelFileName: String,
        downloadUrl: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val outFile = File(modelsDir, modelFileName)
            val tmpFile = File(modelsDir, "$modelFileName.tmp")
            val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) {
                error("HTTP ${conn.responseCode}")
            }
            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (outFile.exists()) outFile.delete()
            tmpFile.renameTo(outFile)
            outFile
        }
    }

    suspend fun runImageModel(
        modelFileName: String,
        inputBitmap: Bitmap
    ): Bitmap? = withContext(Dispatchers.Default) {
        val modelBuffer = loadModelBuffer(modelFileName) ?: return@withContext null
        var interpreter: Interpreter? = null
        try {
            val opts = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(modelBuffer, opts)
            val inShape = interpreter.getInputTensor(0).shape() // [1, H, W, 3]
            val outShape = interpreter.getOutputTensor(0).shape() // [1, H, W, 3]
            if (inShape.size < 4 || outShape.size < 4) return@withContext null

            val inH = inShape[1]
            val inW = inShape[2]
            val scaled = Bitmap.createScaledBitmap(inputBitmap, inW, inH, true)

            val inputBuf = ByteBuffer.allocateDirect(4 * inW * inH * 3).order(ByteOrder.nativeOrder())
            val pixels = IntArray(inW * inH)
            scaled.getPixels(pixels, 0, inW, 0, 0, inW, inH)
            if (scaled !== inputBitmap) scaled.recycle()

            for (px in pixels) {
                inputBuf.putFloat(((px shr 16) and 0xFF) / 255f)
                inputBuf.putFloat(((px shr 8) and 0xFF) / 255f)
                inputBuf.putFloat((px and 0xFF) / 255f)
            }
            inputBuf.rewind()

            val isNchw = outShape[1] == 3 && outShape[2] > 3 && outShape[3] > 3
            val outH = if (isNchw) outShape[2] else outShape[1]
            val outW = if (isNchw) outShape[3] else outShape[2]
            val outputBuf = ByteBuffer.allocateDirect(4 * outW * outH * 3).order(ByteOrder.nativeOrder())
            interpreter.run(inputBuf, outputBuf)
            outputBuf.rewind()

            val outPixels = IntArray(outW * outH)
            if (isNchw) {
                val plane = outW * outH
                val floats = FloatArray(plane * 3)
                outputBuf.asFloatBuffer().get(floats)
                for (i in 0 until plane) {
                    val r = (floats[i].coerceIn(0f, 1f) * 255f).toInt()
                    val g = (floats[plane + i].coerceIn(0f, 1f) * 255f).toInt()
                    val b = (floats[plane * 2 + i].coerceIn(0f, 1f) * 255f).toInt()
                    outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            } else {
                for (i in outPixels.indices) {
                    val r = (outputBuf.float.coerceIn(0f, 1f) * 255f).toInt()
                    val g = (outputBuf.float.coerceIn(0f, 1f) * 255f).toInt()
                    val b = (outputBuf.float.coerceIn(0f, 1f) * 255f).toInt()
                    outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888).apply {
                setPixels(outPixels, 0, outW, 0, 0, outW, outH)
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { interpreter?.close() }
        }
    }

    /**
     * Optional backup AI Audio Denoise (.tflite, e.g. audio_denoise.tflite).
     * Processes normalized [-1f..1f] mono PCM chunk; returns null if model isn't bundled yet.
     */
    suspend fun runMattingMask(
        modelFileName: String = "modnet_matting.tflite",
        inputBitmap: Bitmap
    ): Triple<FloatArray, Int, Int>? = withContext(Dispatchers.Default) {
        val modelBuffer = loadModelBuffer(modelFileName) ?: return@withContext null
        var interpreter: Interpreter? = null
        try {
            val opts = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(modelBuffer, opts)
            val inShape = interpreter.getInputTensor(0).shape()
            val outShape = interpreter.getOutputTensor(0).shape()
            if (inShape.size < 4 || outShape.size < 3) return@withContext null

            val inNchw = inShape[1] == 3 && inShape[2] > 3 && inShape[3] > 3
            val inH = if (inNchw) inShape[2] else inShape[1]
            val inW = if (inNchw) inShape[3] else inShape[2]
            val scaled = Bitmap.createScaledBitmap(inputBitmap, inW, inH, true)
            val pixels = IntArray(inW * inH)
            scaled.getPixels(pixels, 0, inW, 0, 0, inW, inH)
            if (scaled !== inputBitmap) scaled.recycle()

            val inputBuf = ByteBuffer.allocateDirect(4 * inW * inH * 3).order(ByteOrder.nativeOrder())
            if (inNchw) {
                val plane = inW * inH
                val floats = FloatArray(plane * 3)
                for (i in 0 until plane) {
                    val px = pixels[i]
                    floats[i] = (((px shr 16) and 0xFF) / 127.5f) - 1f
                    floats[plane + i] = (((px shr 8) and 0xFF) / 127.5f) - 1f
                    floats[plane * 2 + i] = ((px and 0xFF) / 127.5f) - 1f
                }
                inputBuf.asFloatBuffer().put(floats)
            } else {
                for (px in pixels) {
                    inputBuf.putFloat((((px shr 16) and 0xFF) / 127.5f) - 1f)
                    inputBuf.putFloat((((px shr 8) and 0xFF) / 127.5f) - 1f)
                    inputBuf.putFloat(((px and 0xFF) / 127.5f) - 1f)
                }
                inputBuf.rewind()
            }

            val outH = if (outShape.size == 4 && outShape[1] == 1) outShape[2] else outShape[1]
            val outW = if (outShape.size == 4 && outShape[1] == 1) outShape[3] else outShape[2]
            val outCh = if (outShape.size == 4 && outShape[1] != 1) outShape[3] else 1
            val outputBuf = ByteBuffer.allocateDirect(4 * outW * outH * outCh).order(ByteOrder.nativeOrder())
            interpreter.run(inputBuf, outputBuf)
            outputBuf.rewind()
            val outArr = FloatArray(outW * outH)
            val fb = outputBuf.asFloatBuffer()
            for (i in outArr.indices) {
                outArr[i] = if (fb.hasRemaining()) fb.get().coerceIn(0f, 1f) else 0f
                for (skip in 1 until outCh) if (fb.hasRemaining()) fb.get()
            }
            Triple(outArr, outW, outH)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { interpreter?.close() }
        }
    }

    fun runAudioDenoiseChunk(
        modelFileName: String = "audio_denoise.tflite",
        pcmSamples: FloatArray
    ): FloatArray? {
        val modelBuffer = loadModelBuffer(modelFileName) ?: return null
        var interpreter: Interpreter? = null
        return try {
            val opts = Interpreter.Options().apply { setNumThreads(2) }
            interpreter = Interpreter(modelBuffer, opts)
            val inShape = interpreter.getInputTensor(0).shape()
            val expectedLen = inShape.lastOrNull()?.coerceAtLeast(1) ?: pcmSamples.size
            val chunk = FloatArray(expectedLen)
            val copyN = minOf(expectedLen, pcmSamples.size)
            System.arraycopy(pcmSamples, 0, chunk, 0, copyN)

            val inputBuf = ByteBuffer.allocateDirect(4 * expectedLen).order(ByteOrder.nativeOrder())
            for (v in chunk) inputBuf.putFloat(v.coerceIn(-1f, 1f))
            inputBuf.rewind()

            val outputBuf = ByteBuffer.allocateDirect(4 * expectedLen).order(ByteOrder.nativeOrder())
            interpreter.run(inputBuf, outputBuf)
            outputBuf.rewind()

            FloatArray(copyN) { outputBuf.float.coerceIn(-1f, 1f) }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { interpreter?.close() }
        }
    }

    private fun loadModelBuffer(modelFileName: String): ByteBuffer? {
        val localFile = File(modelsDir, modelFileName)
        if (localFile.exists()) {
            return runCatching {
                FileInputStream(localFile).use { fis ->
                    fis.channel.map(FileChannel.MapMode.READ_ONLY, 0, localFile.length())
                }
            }.getOrNull()
        }
        return runCatching {
            context.assets.openFd("models/$modelFileName").use { afd ->
                FileInputStream(afd.fileDescriptor).use { fis ->
                    fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
                }
            }
        }.getOrNull()
    }
}
