package com.aistudio.kbeditor.r7v4qx.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.aistudio.kbeditor.r7v4qx.media.NativeMediaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PhotoEnhancerEngine(private val context: Context) {

    private val liteRt = LiteRtModelEngine(context)

    suspend fun enhancePhotoToFile(
        sourceUri: String,
        sharpness: Float = 0.45f,
        clarityBoost: Float = 1.15f
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri = Uri.parse(sourceUri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (parsedUri.scheme == "file" && parsedUri.path != null) {
                BitmapFactory.decodeFile(parsedUri.path, bounds)
            } else {
                context.contentResolver.openInputStream(parsedUri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
            }
            var sample = 1
            while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) {
                sample *= 2
            }
            val decOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = if (parsedUri.scheme == "file" && parsedUri.path != null) {
                BitmapFactory.decodeFile(parsedUri.path, decOpts)
            } else {
                context.contentResolver.openInputStream(parsedUri)?.use {
                    BitmapFactory.decodeStream(it, null, decOpts)
                }
            } ?: error("Could not decode photo")

            // Upscale up to 1440px width for crisp HD/2K output without OOM
            val targetW = (raw.width * 1.5f).toInt().coerceIn(960, 1920)
            val targetH = (targetW * (raw.height.toFloat() / raw.width.coerceAtLeast(1))).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, true)
            if (scaled !== raw) raw.recycle()

            if (liteRt.hasModel("photo_enhance.tflite")) {
                val aiOut = liteRt.runImageModel("photo_enhance.tflite", scaled)
                if (aiOut != null) {
                    val aspectRestored = if (aiOut.width != targetW || aiOut.height != targetH) {
                        Bitmap.createScaledBitmap(aiOut, targetW, targetH, true).also { aiOut.recycle() }
                    } else {
                        aiOut
                    }
                    scaled.recycle()
                    if (NativeMediaEngine.isNativeAvailable) {
                        NativeMediaEngine.nativeEnhanceBitmap(aspectRestored, clarityBoost.coerceAtMost(1.12f), 1.08f, 0.01f)
                    }
                    val outFile = File(context.cacheDir, "kb_enhanced_${System.currentTimeMillis()}.png")
                    FileOutputStream(outFile).use { fos ->
                        aspectRestored.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    }
                    aspectRestored.recycle()
                    return@runCatching Uri.fromFile(outFile).toString()
                }
            }

            val mutableBmp = if (scaled.config == Bitmap.Config.ARGB_8888 && scaled.isMutable) {
                scaled
            } else {
                scaled.copy(Bitmap.Config.ARGB_8888, true).also { scaled.recycle() }
            }
            if (NativeMediaEngine.isNativeAvailable &&
                NativeMediaEngine.nativeEnhanceBitmap(mutableBmp, clarityBoost, 1.12f, 0.02f)
            ) {
                val outFile = File(context.cacheDir, "kb_enhanced_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { fos ->
                    mutableBmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                mutableBmp.recycle()
                return@runCatching Uri.fromFile(outFile).toString()
            }

            val w = mutableBmp.width
            val h = mutableBmp.height
            val pixels = IntArray(w * h)
            val outPixels = IntArray(w * h)
            mutableBmp.getPixels(pixels, 0, w, 0, 0, w, h)
            mutableBmp.recycle()

            val k = sharpness.coerceIn(0f, 0.9f)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val idx = y * w + x
                    val c = pixels[idx]
                    var r = Color.red(c).toFloat()
                    var g = Color.green(c).toFloat()
                    var b = Color.blue(c).toFloat()

                    if (x in 1 until (w - 1) && y in 1 until (h - 1)) {
                        val up = pixels[(y - 1) * w + x]
                        val dn = pixels[(y + 1) * w + x]
                        val lf = pixels[y * w + (x - 1)]
                        val rt = pixels[y * w + (x + 1)]

                        val avgR = (Color.red(up) + Color.red(dn) + Color.red(lf) + Color.red(rt)) * 0.25f
                        val avgG = (Color.green(up) + Color.green(dn) + Color.green(lf) + Color.green(rt)) * 0.25f
                        val avgB = (Color.blue(up) + Color.blue(dn) + Color.blue(lf) + Color.blue(rt)) * 0.25f

                        r += (r - avgR) * k
                        g += (g - avgG) * k
                        b += (b - avgB) * k
                    }

                    // Adaptive shadow lift + S-curve clarity & vibrance boost
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b
                    val shadowLift = if (luma < 90f) (90f - luma) * 0.14f else 0f
                    r = ((r + shadowLift - 128f) * clarityBoost + 128f).coerceIn(0f, 255f)
                    g = ((g + shadowLift - 128f) * clarityBoost + 128f).coerceIn(0f, 255f)
                    b = ((b + shadowLift - 128f) * clarityBoost + 128f).coerceIn(0f, 255f)

                    outPixels[idx] = Color.rgb(r.toInt(), g.toInt(), b.toInt())
                }
            }

            val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            outBmp.setPixels(outPixels, 0, w, 0, 0, w, h)

            val outFile = File(context.cacheDir, "kb_enhanced_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                outBmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            outBmp.recycle()
            Uri.fromFile(outFile).toString()
        }
    }
}
