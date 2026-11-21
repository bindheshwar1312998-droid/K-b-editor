package com.aistudio.kbeditor.r7v4qx.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class ProxyMediaEngine(private val context: Context) {

    fun needsProxy(mediaUri: String): Boolean {
        if (mediaUri.isBlank()) return false
        val mmr = MediaMetadataRetriever()
        return try {
            val u = Uri.parse(mediaUri)
            if (u.scheme.isNullOrEmpty()) mmr.setDataSource(mediaUri) else mmr.setDataSource(context, u)
            val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            maxOf(w, h) > 1920
        } catch (_: Throwable) {
            false
        } finally {
            runCatching { mmr.release() }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun generate540pProxy(mediaUri: String): Result<String> = withContext(Dispatchers.Main) {
        val proxyDir = File(context.cacheDir, "proxies").apply { mkdirs() }
        val outFile = File(proxyDir, "proxy_${mediaUri.hashCode().toUInt()}.mp4")
        if (outFile.exists() && outFile.length() > 16_384L) {
            return@withContext Result.success(Uri.fromFile(outFile).toString())
        }
        if (outFile.exists()) outFile.delete()

        suspendCancellableCoroutine { cont ->
            val edited = EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(mediaUri)))
                .setEffects(
                    Effects(
                        emptyList(),
                        listOf(Presentation.createForWidthAndHeight(540, 960, Presentation.LAYOUT_SCALE_TO_FIT))
                    )
                )
                .build()
            val seq = EditedMediaItemSequence(listOf(edited))
            val comp = Composition.Builder(listOf(seq)).build()
            val enc = DefaultEncoderFactory.Builder(context)
                .setEnableFallback(true)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(3_000_000).build()
                )
                .build()
            val transformer = Transformer.Builder(context)
                .setEncoderFactory(enc)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(Result.success(Uri.fromFile(outFile).toString()))
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        runCatching { outFile.delete() }
                        if (cont.isActive) cont.resume(Result.failure(exportException))
                    }
                })
                .build()
            transformer.start(comp, outFile.absolutePath)
            cont.invokeOnCancellation {
                runCatching { transformer.cancel() }
                runCatching { outFile.delete() }
            }
        }
    }
}
