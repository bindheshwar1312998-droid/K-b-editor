package com.aistudio.kbeditor.r7v4qx.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.coroutines.resume

class AiAutoCutEngine {

    private val subjectSegmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
        SubjectSegmentation.getClient(options)
    }

    private val selfieSegmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .build()
        Segmentation.getClient(options)
    }

    suspend fun segmentForeground(
        sourceBitmap: Bitmap,
        backdropColorHex: Long
    ): Bitmap? = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(sourceBitmap, 0)

        // 1. Try Subject Segmentation first (cuts out Cars, Bikes, Pets, Products, People — anything!)
        val subjectMask = suspendCancellableCoroutine<FloatBuffer?> { cont ->
            subjectSegmenter.process(inputImage)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.foregroundConfidenceMask)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }

        if (subjectMask != null) {
            return@withContext applyMask(
                sourceBitmap = sourceBitmap,
                maskW = sourceBitmap.width,
                maskH = sourceBitmap.height,
                buffer = subjectMask,
                backdropColorHex = backdropColorHex
            )
        }

        // 2. Fallback to Selfie Segmenter if Play Services Subject model is still downloading
        val maskResult = suspendCancellableCoroutine<SegmentationMask?> { cont ->
            selfieSegmenter.process(inputImage)
                .addOnSuccessListener { mask ->
                    if (cont.isActive) cont.resume(mask)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } ?: return@withContext null

        applyMask(
            sourceBitmap = sourceBitmap,
            maskW = maskResult.width,
            maskH = maskResult.height,
            buffer = maskResult.buffer.asFloatBuffer(),
            backdropColorHex = backdropColorHex
        )
    }

    private fun applyMask(
        sourceBitmap: Bitmap,
        maskW: Int,
        maskH: Int,
        buffer: FloatBuffer,
        backdropColorHex: Long
    ): Bitmap {
        buffer.rewind()
        val scaled = if (sourceBitmap.width != maskW || sourceBitmap.height != maskH) {
            Bitmap.createScaledBitmap(sourceBitmap, maskW, maskH, true)
        } else {
            sourceBitmap
        }

        val pixels = IntArray(maskW * maskH)
        scaled.getPixels(pixels, 0, maskW, 0, 0, maskW, maskH)

        val bgColor = backdropColorHex.toInt()
        val bgR = Color.red(bgColor)
        val bgG = Color.green(bgColor)
        val bgB = Color.blue(bgColor)

        val total = minOf(pixels.size, buffer.remaining())
        for (i in 0 until total) {
            val confidence = buffer.get().coerceIn(0f, 1f)
            if (confidence < 0.85f) {
                val px = pixels[i]
                val fgR = Color.red(px)
                val fgG = Color.green(px)
                val fgB = Color.blue(px)
                val alpha = ((confidence - 0.48f) / 0.34f).coerceIn(0f, 1f)
                if (backdropColorHex == 0L) {
                    pixels[i] = Color.argb((alpha * 255f).toInt(), fgR, fgG, fgB)
                } else {
                    val r = (fgR * alpha + bgR * (1f - alpha)).toInt()
                    val g = (fgG * alpha + bgG * (1f - alpha)).toInt()
                    val b = (fgB * alpha + bgB * (1f - alpha)).toInt()
                    pixels[i] = Color.argb(255, r, g, b)
                }
            }
        }

        val out = Bitmap.createBitmap(maskW, maskH, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, maskW, 0, 0, maskW, maskH)
        if (scaled !== sourceBitmap) scaled.recycle()
        return out
    }

    fun close() {
        runCatching { subjectSegmenter.close() }
        runCatching { selfieSegmenter.close() }
    }
}
