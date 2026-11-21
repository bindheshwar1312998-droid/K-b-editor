package com.aistudio.kbeditor.r7v4qx.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.media.FaceDetector
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.aistudio.kbeditor.r7v4qx.model.KeyframePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class SmartVisionEngine(private val context: Context) {

    suspend fun trackMotionAcrossClip(
        mediaUri: String,
        startMs: Long,
        endMs: Long,
        targetNormX: Float,
        targetNormY: Float,
        mode: String = "2D",
        sampleCount: Int = 20
    ): List<KeyframePoint> = withContext(Dispatchers.Default) {
        if (mediaUri.isBlank()) {
            return@withContext generateSyntheticTrack(targetNormX, targetNormY, sampleCount)
        }
        val uri = Uri.parse(mediaUri)
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        val isImage = mime.startsWith("image/") ||
            mediaUri.endsWith(".jpg", ignoreCase = true) ||
            mediaUri.endsWith(".jpeg", ignoreCase = true) ||
            mediaUri.endsWith(".png", ignoreCase = true) ||
            mediaUri.endsWith(".webp", ignoreCase = true)
        if (isImage) {
            return@withContext (0 until sampleCount).map { i ->
                val normT = i.toFloat() / (sampleCount - 1).coerceAtLeast(1)
                val driftX = (kotlin.math.sin(normT * Math.PI * 2.0) * 0.015).toFloat()
                val driftY = (kotlin.math.cos(normT * Math.PI * 2.0) * 0.012).toFloat()
                val depthScale = if (mode == "3D") 1.0f + (kotlin.math.sin(normT * Math.PI) * 0.12).toFloat() else 1.0f
                KeyframePoint(
                    normalizedTime = normT,
                    x = (targetNormX + driftX).coerceIn(0.08f, 0.92f),
                    y = (targetNormY + driftY).coerceIn(0.08f, 0.92f),
                    scale = depthScale
                )
            }
        }

        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(context, uri)
            val duration = (endMs - startMs).coerceAtLeast(300L)
            val points = mutableListOf<KeyframePoint>()
            var curX = targetNormX.coerceIn(0.08f, 0.92f)
            var curY = targetNormY.coerceIn(0.08f, 0.92f)
            var prevPatch: IntArray? = null
            val patchRadius = 10
            val patchSize = patchRadius * 2 + 1

            for (i in 0 until sampleCount) {
                val normT = i.toFloat() / (sampleCount - 1).coerceAtLeast(1)
                val timeUs = (startMs + (duration * normT).toLong()) * 1000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val scaled = Bitmap.createScaledBitmap(frame, 240, 240, true)
                    val faceCenter = detectPrimaryFaceNormalized(scaled)
                    if (faceCenter != null && i == 0 && abs(faceCenter.x - targetNormX) < 0.18f && abs(faceCenter.y - targetNormY) < 0.18f) {
                        curX = faceCenter.x
                        curY = faceCenter.y
                    } else if (prevPatch != null) {
                        val matched = matchPatchSad(
                            bitmap = scaled,
                            prevPatch = prevPatch,
                            patchRadius = patchRadius,
                            centerX = (curX * scaled.width).toInt(),
                            centerY = (curY * scaled.height).toInt(),
                            searchRadius = 26
                        )
                        curX = (matched.first.toFloat() / scaled.width).coerceIn(0.08f, 0.92f)
                        curY = (matched.second.toFloat() / scaled.height).coerceIn(0.08f, 0.92f)
                    }
                    prevPatch = extractLumaPatch(
                        scaled,
                        (curX * scaled.width).toInt(),
                        (curY * scaled.height).toInt(),
                        patchRadius,
                        patchSize
                    )
                    if (scaled != frame) scaled.recycle()
                    frame.recycle()
                }
                val depthScale = if (mode == "3D") 1.0f + (kotlin.math.sin(normT * Math.PI) * 0.10).toFloat() else 1.0f
                points.add(
                    KeyframePoint(
                        normalizedTime = normT,
                        x = curX,
                        y = curY,
                        scale = depthScale
                    )
                )
            }
            points
        } catch (_: Exception) {
            generateSyntheticTrack(targetNormX, targetNormY, sampleCount)
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun computeSmartReframeOffset(mediaUri: String, timeMs: Long): Float = withContext(Dispatchers.Default) {
        if (mediaUri.isBlank()) return@withContext 0f
        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(context, Uri.parse(mediaUri))
            val frame = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: return@withContext 0f
            val scaled = Bitmap.createScaledBitmap(frame, 240, 240, true)
            val face = detectPrimaryFaceNormalized(scaled)
            val offset = if (face != null) {
                ((0.5f - face.x) * 1.4f).coerceIn(-0.45f, 0.45f)
            } else {
                computeEdgeSaliencyCenterX(scaled)
            }
            if (scaled != frame) scaled.recycle()
            frame.recycle()
            offset
        } catch (_: Exception) {
            0f
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun detectPrimaryFaceNormalized(bitmap: Bitmap): PointF? {
        val rgb565 = bitmap.copy(Bitmap.Config.RGB_565, false)
        val detector = FaceDetector(rgb565.width, rgb565.height, 1)
        val faces = arrayOfNulls<FaceDetector.Face>(1)
        val count = detector.findFaces(rgb565, faces)
        rgb565.recycle()
        if (count > 0 && faces[0] != null) {
            val mid = PointF()
            faces[0]!!.getMidPoint(mid)
            return PointF(mid.x / bitmap.width, mid.y / bitmap.height)
        }
        return null
    }

    private fun extractLumaPatch(
        bitmap: Bitmap,
        cx: Int,
        cy: Int,
        radius: Int,
        patchSize: Int
    ): IntArray {
        val out = IntArray(patchSize * patchSize)
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val px = (cx + dx).coerceIn(0, bitmap.width - 1)
                val py = (cy + dy).coerceIn(0, bitmap.height - 1)
                val c = bitmap.getPixel(px, py)
                val luma = (((c shr 16) and 0xFF) * 30 + ((c shr 8) and 0xFF) * 59 + (c and 0xFF) * 11) / 100
                out[(dy + radius) * patchSize + (dx + radius)] = luma
            }
        }
        return out
    }

    private fun matchPatchSad(
        bitmap: Bitmap,
        prevPatch: IntArray,
        patchRadius: Int,
        centerX: Int,
        centerY: Int,
        searchRadius: Int
    ): Pair<Int, Int> {
        val patchSize = patchRadius * 2 + 1
        var bestX = centerX
        var bestY = centerY
        var bestSad = Long.MAX_VALUE

        for (sy in (centerY - searchRadius)..(centerY + searchRadius) step 2) {
            for (sx in (centerX - searchRadius)..(centerX + searchRadius) step 2) {
                if (sx !in patchRadius until (bitmap.width - patchRadius) ||
                    sy !in patchRadius until (bitmap.height - patchRadius)
                ) continue
                var sad = 0L
                for (dy in -patchRadius..patchRadius step 2) {
                    for (dx in -patchRadius..patchRadius step 2) {
                        val c = bitmap.getPixel(sx + dx, sy + dy)
                        val luma = (((c shr 16) and 0xFF) * 30 + ((c shr 8) and 0xFF) * 59 + (c and 0xFF) * 11) / 100
                        val ref = prevPatch[(dy + patchRadius) * patchSize + (dx + patchRadius)]
                        sad += abs(luma - ref)
                    }
                }
                if (sad < bestSad) {
                    bestSad = sad
                    bestX = sx
                    bestY = sy
                }
            }
        }
        return bestX to bestY
    }

    private fun computeEdgeSaliencyCenterX(bitmap: Bitmap): Float {
        var weightedSum = 0f
        var totalWeight = 0f
        for (y in 8 until bitmap.height - 8 step 4) {
            for (x in 8 until bitmap.width - 8 step 4) {
                val c1 = bitmap.getPixel(x, y) and 0xFF
                val c2 = bitmap.getPixel(x + 4, y) and 0xFF
                val edge = abs(c1 - c2).toFloat()
                weightedSum += (x.toFloat() / bitmap.width) * edge
                totalWeight += edge
            }
        }
        val center = if (totalWeight > 0f) weightedSum / totalWeight else 0.5f
        return ((0.5f - center) * 1.1f).coerceIn(-0.4f, 0.4f)
    }

    private fun generateSyntheticTrack(
        targetNormX: Float,
        targetNormY: Float,
        sampleCount: Int
    ): List<KeyframePoint> {
        return List(sampleCount) { i ->
            val t = i.toFloat() / (sampleCount - 1).coerceAtLeast(1)
            KeyframePoint(
                normalizedTime = t,
                x = (targetNormX + kotlin.math.sin(t * 3.14f) * 0.04f).coerceIn(0.1f, 0.9f),
                y = (targetNormY + kotlin.math.cos(t * 3.14f) * 0.02f).coerceIn(0.1f, 0.9f),
                scale = 1f
            )
        }
    }
}
