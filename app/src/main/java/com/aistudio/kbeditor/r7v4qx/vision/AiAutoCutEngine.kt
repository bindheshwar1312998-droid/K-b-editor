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
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.coroutines.resume

class AiAutoCutEngine(private val context: android.content.Context? = null) {

    private val liteRt by lazy { context?.let { LiteRtModelEngine(it) } }

    private var prevAlphas: FloatArray? = null
    private var prevW: Int = 0
    private var prevH: Int = 0

    private val subjectSegmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
        SubjectSegmentation.getClient(options)
    }

    private val selfieSegmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        Segmentation.getClient(options)
    }

    suspend fun segmentForeground(
        sourceBitmap: Bitmap,
        backdropColorHex: Long
    ): Bitmap? = withContext(Dispatchers.Default) {
        // 0. If MODNet / RVM TFLite matting model is bundled in assets/models/modnet_matting.tflite, use it for hair-strand alpha matting!
        val modnetRes = liteRt?.takeIf { it.hasModel("modnet_matting.tflite") }?.runMattingMask("modnet_matting.tflite", sourceBitmap)
        if (modnetRes != null) {
            val (mArr, mw, mh) = modnetRes
            val mBuf = java.nio.ByteBuffer.allocateDirect(4 * mArr.size).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(mArr)
                rewind()
            }
            return@withContext applyMaskFullRes(
                sourceBitmap = sourceBitmap,
                maskW = mw,
                maskH = mh,
                buffer = mBuf,
                secondaryBuffer = null,
                secW = sourceBitmap.width,
                secH = sourceBitmap.height,
                backdropColorHex = backdropColorHex
            )
        }

        val inputImage = InputImage.fromBitmap(sourceBitmap, 0)

        val selfieMask = suspendCancellableCoroutine<SegmentationMask?> { cont ->
            selfieSegmenter.process(inputImage)
                .addOnSuccessListener { mask ->
                    if (cont.isActive) cont.resume(mask)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }

        val subjectMask = suspendCancellableCoroutine<FloatBuffer?> { cont ->
            subjectSegmenter.process(inputImage)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.foregroundConfidenceMask)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }

        if (selfieMask != null) {
            val selfieBuf = selfieMask.buffer.duplicate().order(ByteOrder.nativeOrder()).asFloatBuffer()
            return@withContext applyMaskFullRes(
                sourceBitmap = sourceBitmap,
                maskW = selfieMask.width,
                maskH = selfieMask.height,
                buffer = selfieBuf,
                secondaryBuffer = subjectMask,
                secW = sourceBitmap.width,
                secH = sourceBitmap.height,
                backdropColorHex = backdropColorHex
            )
        }

        if (subjectMask != null) {
            return@withContext applyMaskFullRes(
                sourceBitmap = sourceBitmap,
                maskW = sourceBitmap.width,
                maskH = sourceBitmap.height,
                buffer = subjectMask,
                secondaryBuffer = null,
                secW = sourceBitmap.width,
                secH = sourceBitmap.height,
                backdropColorHex = backdropColorHex
            )
        }

        null
    }

    private fun sampleMaskBilinear(buf: FloatArray, mw: Int, mh: Int, fx: Float, fy: Float): Float {
        val x0 = fx.toInt().coerceIn(0, mw - 1)
        val y0 = fy.toInt().coerceIn(0, mh - 1)
        val x1 = (x0 + 1).coerceAtMost(mw - 1)
        val y1 = (y0 + 1).coerceAtMost(mh - 1)
        val dx = (fx - x0).coerceIn(0f, 1f)
        val dy = (fy - y0).coerceIn(0f, 1f)
        val v00 = buf[y0 * mw + x0]
        val v10 = buf[y0 * mw + x1]
        val v01 = buf[y1 * mw + x0]
        val v11 = buf[y1 * mw + x1]
        val top = v00 + (v10 - v00) * dx
        val bot = v01 + (v11 - v01) * dx
        return top + (bot - top) * dy
    }

    private fun applyMaskFullRes(
        sourceBitmap: Bitmap,
        maskW: Int,
        maskH: Int,
        buffer: FloatBuffer,
        secondaryBuffer: FloatBuffer?,
        secW: Int,
        secH: Int,
        backdropColorHex: Long
    ): Bitmap {
        val outW = sourceBitmap.width.coerceAtLeast(1)
        val outH = sourceBitmap.height.coerceAtLeast(1)

        buffer.rewind()
        val m1 = FloatArray(maskW * maskH)
        val n1 = minOf(m1.size, buffer.remaining())
        for (i in 0 until n1) m1[i] = buffer.get().coerceIn(0f, 1f)

        val m2 = if (secondaryBuffer != null) {
            secondaryBuffer.rewind()
            FloatArray(secW * secH).also { arr ->
                val n2 = minOf(arr.size, secondaryBuffer.remaining())
                for (i in 0 until n2) arr[i] = secondaryBuffer.get().coerceIn(0f, 1f)
            }
        } else null

        val pixels = IntArray(outW * outH)
        sourceBitmap.getPixels(pixels, 0, outW, 0, 0, outW, outH)

        val bgColor = backdropColorHex.toInt()
        val bgR = Color.red(bgColor)
        val bgG = Color.green(bgColor)
        val bgB = Color.blue(bgColor)

        val alphas = FloatArray(outW * outH)
        for (y in 0 until outH) {
            val fy1 = (y.toFloat() / outH) * maskH
            val fy2 = (y.toFloat() / outH) * secH
            for (x in 0 until outW) {
                val fx1 = (x.toFloat() / outW) * maskW
                val c1 = sampleMaskBilinear(m1, maskW, maskH, fx1, fy1)
                val c2 = if (m2 != null) {
                    val fx2 = (x.toFloat() / outW) * secW
                    sampleMaskBilinear(m2, secW, secH, fx2, fy2)
                } else 0f
                val confidence = maxOf(c1, c2 * 0.90f)
                alphas[y * outW + x] = if (confidence >= 0.84f) 1f else ((confidence - 0.56f) / 0.28f).coerceIn(0f, 1f)
            }
        }

        // Adaptive Temporal Smoothing: removes frame-to-frame edge flicker without ghosting on fast motion
        val prev = prevAlphas
        if (prev != null && prevW == outW && prevH == outH && prev.size == alphas.size) {
            for (i in alphas.indices) {
                val cur = alphas[i]
                val old = prev[i]
                if (kotlin.math.abs(cur - old) <= 0.35f) {
                    alphas[i] = (cur * 0.72f + old * 0.28f).coerceIn(0f, 1f)
                }
            }
        }
        prevAlphas = alphas.copyOf()
        prevW = outW
        prevH = outH

        // Edge Color Decontamination: replaces green/brown background bleed on hair & shoulder edges with pure foreground color
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                val idx = y * outW + x
                val alpha = alphas[idx]
                if (alpha < 0.99f) {
                    val px = pixels[idx]
                    var fgR = Color.red(px)
                    var fgG = Color.green(px)
                    var fgB = Color.blue(px)
                    if (alpha > 0.06f && alpha < 0.92f) {
                        var sumR = 0; var sumG = 0; var sumB = 0; var cnt = 0
                        val yMin = (y - 2).coerceAtLeast(0)
                        val yMax = (y + 2).coerceAtMost(outH - 1)
                        val xMin = (x - 2).coerceAtLeast(0)
                        val xMax = (x + 2).coerceAtMost(outW - 1)
                        for (ny in yMin..yMax) {
                            for (nx in xMin..xMax) {
                                val nIdx = ny * outW + nx
                                if (alphas[nIdx] >= 0.92f) {
                                    val npx = pixels[nIdx]
                                    sumR += Color.red(npx)
                                    sumG += Color.green(npx)
                                    sumB += Color.blue(npx)
                                    cnt++
                                }
                            }
                        }
                        if (cnt > 0) {
                            fgR = (fgR * 0.40f + (sumR / cnt) * 0.60f).toInt().coerceIn(0, 255)
                            fgG = (fgG * 0.40f + (sumG / cnt) * 0.60f).toInt().coerceIn(0, 255)
                            fgB = (fgB * 0.40f + (sumB / cnt) * 0.60f).toInt().coerceIn(0, 255)
                        }
                    }
                    if (backdropColorHex == 0L) {
                        pixels[idx] = Color.argb((alpha * 255f).toInt(), fgR, fgG, fgB)
                    } else {
                        val r = (fgR * alpha + bgR * (1f - alpha)).toInt()
                        val g = (fgG * alpha + bgG * (1f - alpha)).toInt()
                        val b = (fgB * alpha + bgB * (1f - alpha)).toInt()
                        pixels[idx] = Color.argb(255, r, g, b)
                    }
                }
            }
        }

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, outW, 0, 0, outW, outH)
        return out
    }

    fun close() {
        prevAlphas = null
        runCatching { subjectSegmenter.close() }
        runCatching { selfieSegmenter.close() }
    }
}
