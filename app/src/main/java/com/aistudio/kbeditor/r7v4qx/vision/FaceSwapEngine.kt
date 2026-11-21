package com.aistudio.kbeditor.r7v4qx.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.hypot

data class DetectedFaceBox(
    val normCenterX: Float,
    val normCenterY: Float,
    val normWidth: Float,
    val normHeight: Float,
    val headEulerZ: Float
)

data class PreparedSourceFace(
    val bitmap: Bitmap,
    val leftEye: PointF,
    val rightEye: PointF,
    val noseBase: PointF,
    val mouthCenter: PointF,
    val contourPts: List<PointF>,
    val meanR: Float,
    val meanG: Float,
    val meanB: Float
)

class FaceSwapEngine(private val context: Context) {

    private val detector by lazy {
        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        FaceDetection.getClient(opts)
    }

    private suspend fun detectFacesRaw(bitmap: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { list ->
                    if (cont.isActive) cont.resume(list)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(emptyList())
                }
        }
    }

    private fun extractKeyPoints(face: Face, w: Float, h: Float): Array<PointF> {
        val box = face.boundingBox
        val cx = box.exactCenterX().coerceIn(0f, w)
        val cy = box.exactCenterY().coerceIn(0f, h)
        val bw = box.width().toFloat().coerceAtLeast(20f)
        val bh = box.height().toFloat().coerceAtLeast(20f)

        val lEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            ?: PointF(cx - bw * 0.20f, cy - bh * 0.14f)
        val rEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            ?: PointF(cx + bw * 0.20f, cy - bh * 0.14f)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
            ?: PointF(cx, cy + bh * 0.05f)
        val mLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouth = if (mLeft != null && mRight != null) {
            PointF((mLeft.x + mRight.x) * 0.5f, (mLeft.y + mRight.y) * 0.5f)
        } else {
            face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                ?: PointF(cx, cy + bh * 0.28f)
        }
        return arrayOf(lEye, rEye, nose, mouth)
    }

    private fun sampleInnerFaceRgb(bmp: Bitmap, cx: Float, cy: Float, radius: Float): Triple<Float, Float, Float> {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0
        val rad = radius.toInt().coerceIn(4, 48)
        val ix = cx.toInt()
        val iy = cy.toInt()
        val step = (rad / 4).coerceAtLeast(1)
        for (y in (iy - rad)..(iy + rad) step step) {
            if (y !in 0 until bmp.height) continue
            for (x in (ix - rad)..(ix + rad) step step) {
                if (x !in 0 until bmp.width) continue
                val px = bmp.getPixel(x, y)
                rSum += Color.red(px)
                gSum += Color.green(px)
                bSum += Color.blue(px)
                count++
            }
        }
        if (count == 0) return Triple(128f, 110f, 98f)
        return Triple(rSum.toFloat() / count, gSum.toFloat() / count, bSum.toFloat() / count)
    }

    suspend fun detectPrimaryFace(bitmap: Bitmap): DetectedFaceBox? {
        val faces = detectFacesRaw(bitmap)
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return null
        val box = face.boundingBox
        val w = bitmap.width.toFloat().coerceAtLeast(1f)
        val h = bitmap.height.toFloat().coerceAtLeast(1f)
        return DetectedFaceBox(
            normCenterX = (box.exactCenterX() / w).coerceIn(0.1f, 0.9f),
            normCenterY = (box.exactCenterY() / h).coerceIn(0.1f, 0.9f),
            normWidth = (box.width() / w).coerceIn(0.12f, 0.85f),
            normHeight = (box.height() / h).coerceIn(0.12f, 0.85f),
            headEulerZ = face.headEulerAngleZ
        )
    }

    suspend fun prepareSourceFaceProfile(faceUriStr: String): PreparedSourceFace? = withContext(Dispatchers.IO) {
        if (faceUriStr.isBlank()) return@withContext null
        val raw = runCatching {
            context.contentResolver.openInputStream(Uri.parse(faceUriStr))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull() ?: return@withContext null

        val maxDim = 512f
        val scale = minOf(maxDim / raw.width.coerceAtLeast(1), maxDim / raw.height.coerceAtLeast(1), 1f)
        val bmp = if (scale < 0.99f) {
            Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(64), (raw.height * scale).toInt().coerceAtLeast(64), true).also {
                if (it !== raw) raw.recycle()
            }
        } else raw

        val faces = detectFacesRaw(bmp)
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        if (face == null) {
            val w = bmp.width.toFloat()
            val h = bmp.height.toFloat()
            val (mr, mg, mb) = sampleInnerFaceRgb(bmp, w * 0.5f, h * 0.5f, minOf(w, h) * 0.15f)
            return@withContext PreparedSourceFace(
                bitmap = bmp,
                leftEye = PointF(w * 0.38f, h * 0.40f),
                rightEye = PointF(w * 0.62f, h * 0.40f),
                noseBase = PointF(w * 0.50f, h * 0.52f),
                mouthCenter = PointF(w * 0.50f, h * 0.66f),
                contourPts = emptyList(),
                meanR = mr, meanG = mg, meanB = mb
            )
        }

        val pts = extractKeyPoints(face, bmp.width.toFloat(), bmp.height.toFloat())
        val contour = face.getContour(FaceContour.FACE)?.points.orEmpty()
        val (mr, mg, mb) = sampleInnerFaceRgb(bmp, pts[2].x, pts[2].y, face.boundingBox.width() * 0.16f)
        PreparedSourceFace(
            bitmap = bmp,
            leftEye = pts[0],
            rightEye = pts[1],
            noseBase = pts[2],
            mouthCenter = pts[3],
            contourPts = contour,
            meanR = mr,
            meanG = mg,
            meanB = mb
        )
    }

    suspend fun renderLandmarkFaceOverlay(
        targetFrame: Bitmap,
        source: PreparedSourceFace,
        outW: Int,
        outH: Int,
        scaleMultiplier: Float = 1.0f,
        blendOpacity: Float = 0.92f
    ): Bitmap? = withContext(Dispatchers.Default) {
        val faces = detectFacesRaw(targetFrame)
        val tgtFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return@withContext null

        val sx = outW.toFloat() / targetFrame.width.coerceAtLeast(1)
        val sy = outH.toFloat() / targetFrame.height.coerceAtLeast(1)
        val tgtPts = extractKeyPoints(tgtFace, targetFrame.width.toFloat(), targetFrame.height.toFloat())

        val tgtLEye = PointF(tgtPts[0].x * sx, tgtPts[0].y * sy)
        val tgtREye = PointF(tgtPts[1].x * sx, tgtPts[1].y * sy)
        val tgtNose = PointF(tgtPts[2].x * sx, tgtPts[2].y * sy)

        val srcEyeDx = (source.rightEye.x - source.leftEye.x).toDouble()
        val srcEyeDy = (source.rightEye.y - source.leftEye.y).toDouble()
        val srcEyeDist = hypot(srcEyeDx, srcEyeDy).toFloat().coerceAtLeast(8f)
        val srcAngle = Math.toDegrees(atan2(srcEyeDy, srcEyeDx)).toFloat()
        val srcMidX = (source.leftEye.x + source.rightEye.x) * 0.5f
        val srcMidY = (source.leftEye.y + source.rightEye.y) * 0.5f

        val tgtEyeDx = (tgtREye.x - tgtLEye.x).toDouble()
        val tgtEyeDy = (tgtREye.y - tgtLEye.y).toDouble()
        val tgtEyeDist = hypot(tgtEyeDx, tgtEyeDy).toFloat().coerceAtLeast(8f)
        val tgtAngle = Math.toDegrees(atan2(tgtEyeDy, tgtEyeDx)).toFloat()
        val tgtMidX = (tgtLEye.x + tgtREye.x) * 0.5f
        val tgtMidY = (tgtLEye.y + tgtREye.y) * 0.5f

        val matchScale = (tgtEyeDist / srcEyeDist) * scaleMultiplier.coerceIn(0.6f, 1.6f)
        val deltaAngle = tgtAngle - srcAngle

        // Local skin-tone color matching
        val (tr, tg, tb) = sampleInnerFaceRgb(targetFrame, tgtPts[2].x, tgtPts[2].y, tgtFace.boundingBox.width() * 0.16f)
        val rGain = (tr / source.meanR.coerceAtLeast(24f)).coerceIn(0.72f, 1.42f)
        val gGain = (tg / source.meanG.coerceAtLeast(24f)).coerceIn(0.72f, 1.42f)
        val bGain = (tb / source.meanB.coerceAtLeast(24f)).coerceIn(0.72f, 1.42f)
        val colorCm = ColorMatrix(
            floatArrayOf(
                rGain, 0f, 0f, 0f, 0f,
                0f, gGain, 0f, 0f, 0f,
                0f, 0f, bGain, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val outBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBmp)

        val alignMatrix = Matrix().apply {
            postTranslate(-srcMidX, -srcMidY)
            postRotate(deltaAngle)
            postScale(matchScale, matchScale)
            postTranslate(tgtMidX, tgtMidY)
        }

        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorCm)
            alpha = (blendOpacity.coerceIn(0.2f, 1f) * 255).toInt()
        }
        canvas.drawBitmap(source.bitmap, alignMatrix, facePaint)

        // Clip & feather along actual ML Kit target face contour (or oval fallback)
        val maskBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBmp)
        val contour = tgtFace.getContour(FaceContour.FACE)?.points.orEmpty()
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            maskFilter = android.graphics.BlurMaskFilter((tgtEyeDist * 0.22f).coerceIn(6f, 28f), android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        if (contour.size >= 8) {
            val path = Path()
            val inset = 0.90f
            contour.forEachIndexed { idx, pt ->
                val px = tgtNose.x + (pt.x * sx - tgtNose.x) * inset
                val py = tgtNose.y + (pt.y * sy - tgtNose.y) * inset
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            maskCanvas.drawPath(path, maskPaint)
        } else {
            val bw = tgtFace.boundingBox.width() * sx * 0.42f * scaleMultiplier
            val bh = tgtFace.boundingBox.height() * sy * 0.46f * scaleMultiplier
            maskCanvas.drawOval(tgtNose.x - bw, tgtNose.y - bh, tgtNose.x + bw, tgtNose.y + bh, maskPaint)
        }

        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(maskBmp, 0f, 0f, xferPaint)
        maskBmp.recycle()
        outBmp
    }

    suspend fun prepareFeatheredFaceBitmap(faceUriStr: String): Bitmap? = withContext(Dispatchers.IO) {
        if (faceUriStr.isBlank()) return@withContext null
        val raw = runCatching {
            context.contentResolver.openInputStream(Uri.parse(faceUriStr))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull() ?: return@withContext null

        val faceBox = detectPrimaryFace(raw)
        val cropX: Int
        val cropY: Int
        val cropW: Int
        val cropH: Int
        if (faceBox != null) {
            cropW = (raw.width * faceBox.normWidth * 0.90f).toInt().coerceIn(48, raw.width)
            cropH = (raw.height * faceBox.normHeight * 1.02f).toInt().coerceIn(48, raw.height)
            cropX = ((raw.width * faceBox.normCenterX) - cropW / 2f).toInt().coerceIn(0, (raw.width - cropW).coerceAtLeast(0))
            cropY = ((raw.height * (faceBox.normCenterY + faceBox.normHeight * 0.04f)) - cropH / 2f).toInt().coerceIn(0, (raw.height - cropH).coerceAtLeast(0))
        } else {
            val side = minOf(raw.width, raw.height)
            cropX = (raw.width - side) / 2
            cropY = (raw.height - side) / 2
            cropW = side
            cropH = side
        }

        val cropped = Bitmap.createBitmap(raw, cropX, cropY, cropW, cropH)
        val outSize = 256
        val scaled = Bitmap.createScaledBitmap(cropped, outSize, outSize, true)
        if (cropped !== raw) cropped.recycle()
        raw.recycle()

        val result = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        scaled.recycle()

        val ovalMatrix = Matrix().apply {
            setScale(0.84f, 1.0f, outSize / 2f, outSize / 2f)
        }
        val grad = RadialGradient(
            outSize / 2f,
            outSize / 2f,
            outSize * 0.46f,
            intArrayOf(0xFFFFFFFF.toInt(), 0xDDFFFFFF.toInt(), 0x55FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.44f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        ).apply { setLocalMatrix(ovalMatrix) }
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = grad
        }
        canvas.drawRect(0f, 0f, outSize.toFloat(), outSize.toFloat(), maskPaint)
        result
    }

    fun close() {
        runCatching { detector.close() }
    }
}
