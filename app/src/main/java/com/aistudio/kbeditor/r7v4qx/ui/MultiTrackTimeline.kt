package com.aistudio.kbeditor.r7v4qx.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aistudio.kbeditor.r7v4qx.model.KbEditorProject
import com.aistudio.kbeditor.r7v4qx.model.TrackState
import com.aistudio.kbeditor.r7v4qx.ui.theme.*
import kotlin.math.roundToLong

@Composable
fun MultiTrackTimeline(
    project: KbEditorProject,
    playheadMs: Long,
    selectedVideoIndex: Int,
    onSelectVideo: (Int) -> Unit,
    selectedPipId: String? = null,
    onSelectPip: (String?) -> Unit = {},
    onMovePipTiming: (String, Long) -> Unit = { _, _ -> },
    onMoveCaptionTiming: (String, Long) -> Unit = { _, _ -> },
    onTrimClipStart: (Int, Long) -> Unit = { _, _ -> },
    onTrimClipEnd: (Int, Long) -> Unit = { _, _ -> },
    onSeekMs: (Long) -> Unit,
    onToggleTrack: ((TrackState) -> TrackState) -> Unit,
    onOpenSheet: (ActiveStudioSheet) -> Unit,
    onSplitClip: () -> Unit,
    onRippleDelete: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onAddClip: () -> Unit = {},
    onDuplicateClip: () -> Unit = {},
    onFlipClip: () -> Unit = {},
    onFreezeFrame: () -> Unit = {},
    isProAiMode: Boolean = false,
    onToggleProAiMode: (Boolean) -> Unit = {},
    onPhotoToVideo: () -> Unit = {},
    onRotateClip: () -> Unit = {},
    onReplaceClip: () -> Unit = {},
    isMagicEraserMode: Boolean = false,
    onToggleMagicEraser: () -> Unit = {},
    onClearMagicEraser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var timelineZoom by remember { mutableFloatStateOf(1f) }
    val pxPerSecDp = (56f * timelineZoom).dp
    val pxPerSecPx = with(density) { pxPerSecDp.toPx() }
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val halfWidthDp = with(density) { (viewportWidthPx / 2f).toDp() }

    val clipWidthsDp = remember(project.videoClips, timelineZoom) {
        project.videoClips.map { clip ->
            (clip.activeDurationMs / 1000f * pxPerSecDp.value).coerceAtLeast(64f)
        }
    }
    val transitionsCount = (project.videoClips.size - 1).coerceAtLeast(0)
    val exactTrackWidthDpVal = (clipWidthsDp.sum() + transitionsCount * 22f).coerceAtLeast(pxPerSecDp.value)
    val totalSec = (project.totalDurationMs / 1000f).coerceAtLeast(1f)
    val totalTrackWidthDp = exactTrackWidthDpVal.dp
    val exactTrackWidthPx = with(density) { totalTrackWidthDp.toPx() }

    // Sync scroll position when playheadMs changes externally (e.g. during playback)
    LaunchedEffect(playheadMs, exactTrackWidthPx, project.totalDurationMs) {
        if (!scrollState.isScrollInProgress && project.totalDurationMs > 0L) {
            val frac = (playheadMs.toFloat() / project.totalDurationMs.toFloat()).coerceIn(0f, 1f)
            val targetPx = (frac * exactTrackWidthPx).toInt().coerceIn(0, scrollState.maxValue)
            if (kotlin.math.abs(scrollState.value - targetPx) > 4) {
                scrollState.scrollTo(targetPx)
            }
        }
    }

    // Update playheadMs when user drags timeline horizontally under the center playhead
    LaunchedEffect(scrollState.value, scrollState.isScrollInProgress, exactTrackWidthPx, project.totalDurationMs) {
        if (scrollState.isScrollInProgress && exactTrackWidthPx > 0f) {
            val frac = (scrollState.value.toFloat() / exactTrackWidthPx).coerceIn(0f, 1f)
            val ms = (frac * project.totalDurationMs.toFloat()).roundToLong()
                .coerceIn(0L, project.totalDurationMs)
            onSeekMs(ms)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0C10))
    ) {
        // CapCut-style Timeline Canvas with Center White Playhead
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .onSizeChanged { viewportWidthPx = it.width }
        ) {
            // Left Mute Button (CapCut style)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 6.dp, top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC181B22))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "−",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { timelineZoom = (timelineZoom - 0.25f).coerceIn(0.4f, 3.5f) }
                        .padding(horizontal = 4.dp)
                )
                Text("${(timelineZoom * 100).toInt()}%", color = ElectricCyan, fontSize = 9.sp)
                Text(
                    "+",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { timelineZoom = (timelineZoom + 0.25f).coerceIn(0.4f, 3.5f) }
                        .padding(horizontal = 4.dp)
                )
            }


            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 34.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF181B22))
                    .clickable { onToggleTrack { it.copy(a1Muted = !it.a1Muted) } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (project.tracks.a1Muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute Audio",
                    tint = if (project.tracks.a1Muted) CoralAmber else TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Scrollable Tracks under fixed center playhead
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(halfWidthDp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 37.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1F242E))
                            .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                            .clickable { onAddClip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Clip", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .width(totalTrackWidthDp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 1. Time Ruler (00:00, 00:01, 00:02...)
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .fillMaxWidth()
                    ) {
                        val secCount = totalSec.toInt().coerceAtLeast(1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (s in 0..secCount) {
                                Box(modifier = Modifier.width(pxPerSecDp)) {
                                    Text(
                                        text = "%02d:%02d".format(s / 60, s % 60),
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // 2. Main Video Track (V1) with Filmstrip Thumbnails & White Selection Border
                    Row(
                        modifier = Modifier.height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        project.videoClips.forEachIndexed { idx, clip ->
                            val clipWidthDp = (clip.activeDurationMs / 1000f * pxPerSecDp.value).coerceAtLeast(64f).dp
                            val selected = idx == selectedVideoIndex
                            Box(
                                modifier = Modifier
                                    .width(clipWidthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1A202C))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) Color.White else SlateBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .pointerInput(clip.id, idx, project.videoClips.size) {
                                        var dragAcc = 0f
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragAcc = 0f
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onSelectVideo(idx)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAcc += dragAmount.x
                                                val thresholdPx = with(density) { 44.dp.toPx() }
                                                if (dragAcc > thresholdPx) {
                                                    dragAcc = 0f
                                                    onMoveClip(1)
                                                } else if (dragAcc < -thresholdPx) {
                                                    dragAcc = 0f
                                                    onMoveClip(-1)
                                                }
                                            }
                                        )
                                    }
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectVideo(idx)
                                    }
                            ) {
                                // Filmstrip frames
                                if (clip.mediaUri.isNotBlank()) {
                                    val thumbCount = (clipWidthDp.value / 56f).toInt().coerceIn(2, 24)
                                    val isImageMedia = remember(clip.mediaUri, clip.isAiGeneratedBroll) {
                                        clip.isAiGeneratedBroll ||
                                            clip.mediaUri.endsWith(".png", true) ||
                                            clip.mediaUri.endsWith(".jpg", true) ||
                                            clip.mediaUri.endsWith(".jpeg", true) ||
                                            clip.mediaUri.endsWith(".webp", true) ||
                                            runCatching {
                                                context.contentResolver.getType(Uri.parse(clip.mediaUri))?.startsWith("image/") == true
                                            }.getOrDefault(false)
                                    }
                                    val videoFrames by produceState<List<Bitmap>>(
                                        initialValue = emptyList(),
                                        key1 = clip.id,
                                        key2 = clip.mediaUri,
                                        key3 = clip.trimStartMs to clip.trimEndMs
                                    ) {
                                        if (!isImageMedia) {
                                            value = withContext(Dispatchers.IO) {
                                                extractFilmstripFrames(
                                                    context = context,
                                                    mediaUri = clip.mediaUri,
                                                    trimStartMs = clip.trimStartMs,
                                                    trimEndMs = clip.trimEndMs,
                                                    frameCount = 5
                                                )
                                            }
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        if (videoFrames.isNotEmpty()) {
                                            repeat(thumbCount) { i ->
                                                val bmp = videoFrames[(i * videoFrames.size / thumbCount).coerceIn(0, videoFrames.lastIndex)]
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                )
                                            }
                                        } else {
                                            repeat(thumbCount) {
                                                AsyncImage(
                                                    model = clip.mediaUri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                )
                                            }
                                        }
                                    }
                                }
                                // Left & Right white trim handles when selected
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(8.dp)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(8.dp)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                    )
                                }
                                // CapCut-style Keyframe Diamond (◆) markers on clip
                                if (clip.keyframes.isNotEmpty()) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        clip.keyframes.forEach { kf ->
                                            val kx = kf.normalizedTime.coerceIn(0.03f, 0.97f) * size.width
                                            val ky = size.height * 0.5f
                                            val r = 7.dp.toPx()
                                            val diamond = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(kx, ky - r)
                                                lineTo(kx + r, ky)
                                                lineTo(kx, ky + r)
                                                lineTo(kx - r, ky)
                                                close()
                                            }
                                            drawPath(diamond, color = WarmAmber)
                                            drawPath(diamond, color = Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                                        }
                                    }
                                }
                                // Clip label pill
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 10.dp, top = 2.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} • ${"%.1f".format(clip.activeDurationMs / 1000f)}s",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (idx < project.videoClips.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .clickable { onOpenSheet(ActiveStudioSheet.TRANSITION_FX) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("/", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }


                    }

                    // 3. Overlay / PiP Track (O1) - CapCut-style positioned & draggable
                    Box(
                        modifier = Modifier
                            .width(totalTrackWidthDp)
                            .height(26.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (project.pipClips.isEmpty()) {
                            CapCutSubTrackPill("+ Add Overlay / PiP", VioletClip) {
                                onOpenSheet(ActiveStudioSheet.PIP_OVERLAY)
                            }
                        } else {
                            val totalDurMs = project.totalDurationMs.coerceAtLeast(1000L)
                            project.pipClips.forEach { pip ->
                                val isSel = pip.id == selectedPipId
                                val startFrac = (pip.startMs.toFloat() / totalDurMs.toFloat()).coerceIn(0f, 0.96f)
                                val durFrac = (pip.durationMs.toFloat() / totalDurMs.toFloat()).coerceIn(0.05f, 1f - startFrac)
                                val xDp = totalTrackWidthDp * startFrac
                                val wDp = (totalTrackWidthDp * durFrac).coerceAtLeast(64.dp)
                                Box(
                                    modifier = Modifier
                                        .offset(x = xDp)
                                        .width(wDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isSel) VioletClip.copy(alpha = 0.55f) else VioletClip.copy(alpha = 0.28f))
                                        .border(if (isSel) 1.5.dp else 1.dp, if (isSel) Color.White else VioletClip, RoundedCornerShape(5.dp))
                                        .pointerInput(pip.id, totalDurMs, totalTrackWidthDp) {
                                            detectHorizontalDragGestures { change, dragAmount ->
                                                change.consume()
                                                val totalPx = with(density) { totalTrackWidthDp.toPx() }.coerceAtLeast(1f)
                                                val deltaMs = ((dragAmount / totalPx) * totalDurMs).roundToLong()
                                                if (deltaMs != 0L) onMovePipTiming(pip.id, deltaMs)
                                            }
                                        }
                                        .clickable {
                                            if (isSel) onOpenSheet(ActiveStudioSheet.PIP_OVERLAY) else onSelectPip(pip.id)
                                        }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(pip.title, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }

                    // 4. Text / Caption Track (T1) - CapCut-style positioned & draggable
                    Box(
                        modifier = Modifier
                            .width(totalTrackWidthDp)
                            .height(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (project.captionClips.isEmpty()) {
                            CapCutSubTrackPill("+ Add Text", WarmAmber) {
                                onOpenSheet(ActiveStudioSheet.CAPTION_3D)
                            }
                        } else {
                            val totalDurMs = project.totalDurationMs.coerceAtLeast(1000L)
                            project.captionClips.forEach { cap ->
                                val startFrac = (cap.startMs.toFloat() / totalDurMs.toFloat()).coerceIn(0f, 0.96f)
                                val durFrac = (cap.durationMs.toFloat() / totalDurMs.toFloat()).coerceIn(0.05f, 1f - startFrac)
                                val xDp = totalTrackWidthDp * startFrac
                                val wDp = (totalTrackWidthDp * durFrac).coerceAtLeast(64.dp)
                                Box(
                                    modifier = Modifier
                                        .offset(x = xDp)
                                        .width(wDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(WarmAmber.copy(alpha = 0.32f))
                                        .border(1.dp, WarmAmber, RoundedCornerShape(5.dp))
                                        .pointerInput(cap.id, totalDurMs, totalTrackWidthDp) {
                                            detectHorizontalDragGestures { change, dragAmount ->
                                                change.consume()
                                                val totalPx = with(density) { totalTrackWidthDp.toPx() }.coerceAtLeast(1f)
                                                val deltaMs = ((dragAmount / totalPx) * totalDurMs).roundToLong()
                                                if (deltaMs != 0L) onMoveCaptionTiming(cap.id, deltaMs)
                                            }
                                        }
                                        .clickable { onOpenSheet(ActiveStudioSheet.CAPTION_3D) }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text("T  ${cap.text}", fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }

                    // 5. Audio Track (A1) with Waveform Bars
                    Row(
                        modifier = Modifier.height(26.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (project.audioClips.isEmpty()) {
                            CapCutSubTrackPill("+ Add Audio / Voiceover", EmeraldTrack) {
                                onOpenSheet(ActiveStudioSheet.AUDIO_PRO)
                            }
                        } else {
                            project.audioClips.forEach { audio ->
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(EmeraldTrack.copy(alpha = 0.24f))
                                        .border(1.dp, EmeraldTrack, RoundedCornerShape(5.dp))
                                        .clickable { onOpenSheet(ActiveStudioSheet.AUDIO_PRO) }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "♪ ${audio.title}",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(halfWidthDp))
            }

            // Fixed Center White Playhead Line (CapCut signature)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val cx = size.width / 2f
                drawLine(
                    color = Color.White,
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = 3f
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(cx, 5f)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1A1D24))

        // 2-Column Mode Switcher: Pro AI Edit | Manual Edit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0D13))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isProAiMode) VioletClip.copy(alpha = 0.25f) else Color(0xFF141821))
                    .border(1.dp, if (isProAiMode) ElectricCyan else SlateBorder, RoundedCornerShape(8.dp))
                    .clickable { onToggleProAiMode(true) }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨ Pro AI Edit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isProAiMode) ElectricCyan else TextMuted
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isProAiMode) VioletClip.copy(alpha = 0.25f) else Color(0xFF141821))
                    .border(1.dp, if (!isProAiMode) VioletClip else SlateBorder, RoundedCornerShape(8.dp))
                    .clickable { onToggleProAiMode(false) }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✂️ Manual Edit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!isProAiMode) Color.White else TextMuted
                )
            }
        }

        // CapCut-style Bottom Icon-over-Label Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF07080B))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isProAiMode) {
                CapCutToolItem("Photo Video", Icons.Default.Collections, tint = WarmAmber, onClick = onPhotoToVideo)
                CapCutToolItem("One-Prompt", Icons.Default.AutoAwesome, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.AUTO_CREATOR) }
                CapCutToolItem("Enhance", Icons.Default.HighQuality, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.ENHANCER) }
                CapCutToolItem("Auto Color", Icons.Default.AutoFixHigh, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.COLOR_WHEELS) }
                CapCutToolItem("Auto Cap", Icons.Default.ClosedCaption, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.CAPTION_3D) }
                CapCutToolItem("AI Voice", Icons.Default.Mic, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.VOICE_CLONE) }
                CapCutToolItem("Audio Dubbing", Icons.Default.RecordVoiceOver, tint = EmeraldTrack) { onOpenSheet(ActiveStudioSheet.AUDIO_DUBBING) }
                CapCutToolItem("AI Cut/BG", Icons.Default.PersonRemove, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.VFX_CHROMA_MASK) }
                CapCutToolItem("Magic Erase", Icons.Default.CleaningServices, tint = if (isMagicEraserMode) ElectricCyan else WarmAmber, onClick = onToggleMagicEraser)
                CapCutToolItem("De-Watermark", Icons.Default.AutoFixNormal, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.WATERMARK_REMOVER) }
                CapCutToolItem("Face Swap", Icons.Default.Face, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.FACE_SWAP) }
                CapCutToolItem("1100+ SFX", Icons.Default.MusicNote, tint = EmeraldTrack) { onOpenSheet(ActiveStudioSheet.AUDIO_PRO) }
                CapCutToolItem("Stickers", Icons.Default.EmojiEmotions, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.STICKERS) }
                CapCutToolItem("Export 4K", Icons.Default.FileDownload, tint = VioletClip) { onOpenSheet(ActiveStudioSheet.EXPORT_PRO) }
            } else {
            CapCutToolItem("Split", Icons.Default.ContentCut) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSplitClip() }
            CapCutToolItem("Duplicate", Icons.Default.ContentCopy, onClick = onDuplicateClip)
            CapCutToolItem("Volume", Icons.Default.VolumeUp) { onOpenSheet(ActiveStudioSheet.CLIP_EDIT) }
            CapCutToolItem("Rotate", Icons.Default.RotateRight, onClick = onRotateClip)
            CapCutToolItem("Flip", Icons.Default.Flip, onClick = onFlipClip)
            CapCutToolItem("Replace", Icons.Default.SwapHoriz, onClick = onReplaceClip)
            CapCutToolItem("Freeze", Icons.Default.AcUnit, onClick = onFreezeFrame)
            CapCutToolItem("Delete", Icons.Default.DeleteOutline) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRippleDelete() }
            CapCutToolItem("Speed", Icons.Default.Speed) { onOpenSheet(ActiveStudioSheet.SPEED_RAMP) }
            CapCutToolItem("Audio", Icons.Default.MusicNote) { onOpenSheet(ActiveStudioSheet.AUDIO_PRO) }
            CapCutToolItem("Audio Dubbing", Icons.Default.RecordVoiceOver, tint = EmeraldTrack) { onOpenSheet(ActiveStudioSheet.AUDIO_DUBBING) }
            CapCutToolItem("Text", Icons.Default.TextFields) { onOpenSheet(ActiveStudioSheet.CAPTION_3D) }
            CapCutToolItem("Stickers", Icons.Default.EmojiEmotions, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.STICKERS) }
            CapCutToolItem("Overlay", Icons.Default.Layers) { onOpenSheet(ActiveStudioSheet.PIP_OVERLAY) }
            CapCutToolItem("Mask", Icons.Default.FilterCenterFocus) { onOpenSheet(ActiveStudioSheet.VFX_CHROMA_MASK) }
            CapCutToolItem("Magic Erase", Icons.Default.CleaningServices, tint = if (isMagicEraserMode) ElectricCyan else Color.White, onClick = onToggleMagicEraser)
            CapCutToolItem("De-Watermark", Icons.Default.AutoFixNormal) { onOpenSheet(ActiveStudioSheet.WATERMARK_REMOVER) }
            CapCutToolItem("Effects", Icons.Default.Flare) { onOpenSheet(ActiveStudioSheet.TRANSITION_FX) }
            CapCutToolItem("VFX/Cut", Icons.Default.AutoFixHigh) { onOpenSheet(ActiveStudioSheet.VFX_CHROMA_MASK) }
            CapCutToolItem("Enhance", Icons.Default.HighQuality, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.ENHANCER) }
            CapCutToolItem("Adjust", Icons.Default.Tune) { onOpenSheet(ActiveStudioSheet.COLOR_WHEELS) }
            CapCutToolItem("Keyframe", Icons.Default.Diamond, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.KEYFRAME_GRAPH) }
            CapCutToolItem("Track", Icons.Default.MyLocation) { onOpenSheet(ActiveStudioSheet.MOTION_TRACKER) }
            CapCutToolItem("Ratio", Icons.Default.AspectRatio) { onOpenSheet(ActiveStudioSheet.CANVAS_REFRAME) }
            CapCutToolItem("Face Swap", Icons.Default.Face, tint = WarmAmber) { onOpenSheet(ActiveStudioSheet.FACE_SWAP) }
            CapCutToolItem("AI Pro", Icons.Default.AutoAwesome, tint = ElectricCyan) { onOpenSheet(ActiveStudioSheet.AUTO_CREATOR) }
            CapCutToolItem("Move <", Icons.Default.ChevronLeft) { onMoveClip(-1) }
            CapCutToolItem("Move >", Icons.Default.ChevronRight) { onMoveClip(1) }
            }
        }
    }
}

@Composable
private fun CapCutSubTrackPill(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF141820))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, fontSize = 10.sp, color = accent)
    }
}

@Composable
private fun CapCutToolItem(
    label: String,
    icon: ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF121620))
            .border(1.dp, if (tint == Color.White) Color(0xFF222836) else tint.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (tint == Color.White) TextPrimary else tint,
            maxLines = 1
        )
    }
}

private val filmstripSemaphore = kotlinx.coroutines.sync.Semaphore(2)

private suspend fun extractFilmstripFrames(
    context: android.content.Context,
    mediaUri: String,
    trimStartMs: Long,
    trimEndMs: Long,
    frameCount: Int
): List<Bitmap> {
    filmstripSemaphore.acquire()
    val retriever = MediaMetadataRetriever()
    val result = mutableListOf<Bitmap>()
    try {
        val uri = Uri.parse(mediaUri)
        if (uri.scheme.isNullOrEmpty()) {
            retriever.setDataSource(mediaUri)
        } else {
            retriever.setDataSource(context, uri)
        }
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val startMs = trimStartMs.coerceAtLeast(0L)
        val endMs = (if (trimEndMs > startMs) trimEndMs else durationMs).coerceAtLeast(startMs + 300L)
        val spanMs = (endMs - startMs).coerceAtLeast(1L)
        val count = frameCount.coerceIn(1, 6)
        for (i in 0 until count) {
            val frac = if (count == 1) 0f else i.toFloat() / (count - 1).toFloat()
            val timeUs = (startMs + (spanMs * frac).toLong()) * 1000L
            val raw = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 128, 96)
                } else null
            }.getOrNull() ?: runCatching {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { bmp ->
                    Bitmap.createScaledBitmap(bmp, 128, 96, true).also {
                        if (it != bmp) bmp.recycle()
                    }
                }
            }.getOrNull()
            if (raw != null) {
                val lowMem = raw.copy(Bitmap.Config.RGB_565, false)
                if (lowMem != null) {
                    if (lowMem != raw) raw.recycle()
                    result.add(lowMem)
                } else {
                    result.add(raw)
                }
            }
        }
        if (result.isEmpty()) {
            runCatching { retriever.getFrameAtTime(-1L) }?.getOrNull()?.let { first ->
                val scaled = Bitmap.createScaledBitmap(first, 128, 96, true)
                if (scaled != first) first.recycle()
                result.add(scaled)
            }
        }
    } catch (_: Throwable) {
    } finally {
        runCatching { retriever.release() }
        filmstripSemaphore.release()
    }
    return result
}
