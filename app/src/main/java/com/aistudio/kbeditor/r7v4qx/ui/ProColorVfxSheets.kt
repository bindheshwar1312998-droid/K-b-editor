package com.aistudio.kbeditor.r7v4qx.ui

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.Canvas
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.kbeditor.r7v4qx.model.*
import com.aistudio.kbeditor.r7v4qx.ui.theme.*

@Composable
fun SpeedRampSheet(
    clip: VideoClip,
    onUpdateClip: (VideoClip) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Optical Flow Speed Ramping", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        SpeedRampGraph(clip.speedRamp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpeedRampPreset.entries.forEach { preset ->
                FilterChip(
                    selected = clip.speedRamp == preset,
                    onClick = { onUpdateClip(clip.copy(speedRamp = preset)) },
                    label = { Text(preset.label, maxLines = 1) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.1f to "0.1x", 0.25f to "0.25x", 0.5f to "0.5x", 1.0f to "1.0x", 2.0f to "2.0x", 5.0f to "5x", 10.0f to "10x", 20.0f to "20x", 40.0f to "40x", 50.0f to "50x").forEach { (spd, lbl) ->
                FilterChip(
                    selected = kotlin.math.abs(clip.speedMultiplier - spd) < 0.05f,
                    onClick = { onUpdateClip(clip.copy(speedMultiplier = spd, speedRamp = SpeedRampPreset.CONSTANT)) },
                    label = { Text(lbl) }
                )
            }
        }

        SliderRow("Base Speed (0.1x - 50x)", clip.speedMultiplier, 0.1f..50.0f) {
            onUpdateClip(clip.copy(speedMultiplier = it))
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Optical Flow Frame Interpolation", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = clip.opticalFlowEnabled,
                onCheckedChange = { onUpdateClip(clip.copy(opticalFlowEnabled = it)) }
            )
        }
    }
}

@Composable
fun VfxChromaMaskSheet(
    clip: VideoClip,
    onUpdateClip: (VideoClip) -> Unit,
    onEditMaskOnScreen: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mask & Chroma Key Studio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        Text("CapCut-Style Mask Shapes", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MaskType.entries.forEach { mask ->
                CapCutMaskCard(
                    maskType = mask,
                    selected = clip.maskType == mask,
                    onClick = { onUpdateClip(clip.copy(maskType = mask)) }
                )
            }
        }

        if (clip.maskType != MaskType.NONE) {
            Button(
                onClick = onEditMaskOnScreen,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = SlateVoid)
            ) {
                Text("Drag / Pinch / Rotate Mask on Screen", fontWeight = FontWeight.Bold)
            }
        }

        SliderRow("Mask Feather", clip.maskFeather, 0f..0.4f) {
            onUpdateClip(clip.copy(maskFeather = it))
        }
        SliderRow("Mask Size", clip.maskScale, 0.15f..2.2f) {
            onUpdateClip(clip.copy(maskScale = it))
        }
        SliderRow("Mask Width (Oval X)", clip.maskScaleX, 0.3f..2.2f) {
            onUpdateClip(clip.copy(maskScaleX = it))
        }
        SliderRow("Mask Height (Oval Y)", clip.maskScaleY, 0.3f..2.2f) {
            onUpdateClip(clip.copy(maskScaleY = it))
        }
        SliderRow("Mask Position X", clip.maskOffsetX, -0.4f..0.4f) {
            onUpdateClip(clip.copy(maskOffsetX = it))
        }
        SliderRow("Mask Position Y", clip.maskOffsetY, -0.4f..0.4f) {
            onUpdateClip(clip.copy(maskOffsetY = it))
        }
        SliderRow("Mask Rotate", clip.maskRotationDeg, 0f..360f) {
            onUpdateClip(clip.copy(maskRotationDeg = it))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Invert Mask", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = clip.maskInverted,
                onCheckedChange = { onUpdateClip(clip.copy(maskInverted = it)) }
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    onUpdateClip(
                        clip.copy(
                            maskScale = 0.85f,
                            maskScaleX = 1.0f,
                            maskScaleY = 1.0f,
                            maskOffsetX = 0f,
                            maskOffsetY = 0f,
                            maskRotationDeg = 0f,
                            maskFeather = 0.08f,
                            maskInverted = false
                        )
                    )
                }
            ) {
                Text("Reset")
            }
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Chroma Key Extraction", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = clip.chromaKey.enabled,
                onCheckedChange = { onUpdateClip(clip.copy(chromaKey = clip.chromaKey.copy(enabled = it))) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Green", "Blue", "Cyan").forEach { hue ->
                FilterChip(
                    selected = clip.chromaKey.keyHue == hue,
                    onClick = { onUpdateClip(clip.copy(chromaKey = clip.chromaKey.copy(keyHue = hue))) },
                    label = { Text(hue) }
                )
            }
        }

        SliderRow("Key Tolerance", clip.chromaKey.tolerance, 0.05f..0.75f) {
            onUpdateClip(clip.copy(chromaKey = clip.chromaKey.copy(tolerance = it)))
        }
        SliderRow("Spill Suppress", clip.chromaKey.spillSuppression, 0f..1f) {
            onUpdateClip(clip.copy(chromaKey = clip.chromaKey.copy(spillSuppression = it)))
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("AI Auto-Cut Foreground Matting", color = ElectricCyan, modifier = Modifier.weight(1f))
            Switch(
                checked = clip.chromaKey.aiAutoCut,
                onCheckedChange = { onUpdateClip(clip.copy(chromaKey = clip.chromaKey.copy(aiAutoCut = it))) }
            )
        }
    }
}

@Composable
private fun CapCutMaskCard(
    maskType: MaskType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = if (selected) VioletClip else SlateBorder
    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) VioletClip.copy(alpha = 0.22f) else Color(0xFF141820))
            .border(if (selected) 1.5.dp else 1.dp, accent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val strokeColor = if (selected) Color.White else TextMuted
            when (maskType) {
                MaskType.NONE -> {
                    drawCircle(strokeColor, radius = size.minDimension * 0.36f, style = Stroke(2.5f))
                    drawLine(strokeColor, Offset(4f, size.height - 4f), Offset(size.width - 4f, 4f), strokeWidth = 2.5f)
                }
                MaskType.CIRCLE -> {
                    drawCircle(strokeColor, radius = size.minDimension * 0.38f, style = Stroke(3f))
                }
                MaskType.ROUNDED_RECT -> {
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(3f, 5f),
                        size = Size(size.width - 6f, size.height - 10f),
                        cornerRadius = CornerRadius(7f, 7f),
                        style = Stroke(3f)
                    )
                }
                MaskType.SPLIT_WIPE -> {
                    drawLine(strokeColor, Offset(size.width * 0.5f, 2f), Offset(size.width * 0.5f, size.height - 2f), strokeWidth = 3f)
                    drawRect(strokeColor.copy(alpha = 0.35f), topLeft = Offset(2f, 4f), size = Size(size.width * 0.45f, size.height - 8f))
                }
                MaskType.ANAMORPHIC_BARS -> {
                    drawRect(strokeColor, topLeft = Offset(2f, 5f), size = Size(size.width - 4f, size.height - 10f), style = Stroke(2.5f))
                    drawLine(strokeColor, Offset(2f, size.height * 0.3f), Offset(size.width - 2f, size.height * 0.3f), strokeWidth = 2f)
                    drawLine(strokeColor, Offset(2f, size.height * 0.7f), Offset(size.width - 2f, size.height * 0.7f), strokeWidth = 2f)
                }
                MaskType.MIRROR -> {
                    drawLine(strokeColor, Offset(2f, size.height * 0.25f), Offset(size.width - 2f, size.height * 0.25f), strokeWidth = 2.5f)
                    drawLine(strokeColor, Offset(2f, size.height * 0.75f), Offset(size.width - 2f, size.height * 0.75f), strokeWidth = 2.5f)
                    drawRect(strokeColor.copy(alpha = 0.3f), topLeft = Offset(2f, size.height * 0.25f), size = Size(size.width - 4f, size.height * 0.5f))
                }
                MaskType.HEART -> {
                    val w = size.width
                    val h = size.height
                    val p = Path().apply {
                        moveTo(w * 0.5f, h * 0.85f)
                        cubicTo(w * 0.1f, h * 0.58f, w * 0.08f, h * 0.22f, w * 0.32f, h * 0.20f)
                        cubicTo(w * 0.44f, h * 0.18f, w * 0.50f, h * 0.30f, w * 0.50f, h * 0.34f)
                        cubicTo(w * 0.50f, h * 0.30f, w * 0.56f, h * 0.18f, w * 0.68f, h * 0.20f)
                        cubicTo(w * 0.92f, h * 0.22f, w * 0.90f, h * 0.58f, w * 0.5f, h * 0.85f)
                        close()
                    }
                    drawPath(p, color = strokeColor, style = Stroke(2.5f))
                }
                MaskType.STAR -> {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outer = size.minDimension * 0.44f
                    val inner = size.minDimension * 0.19f
                    val p = Path()
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outer else inner
                        val angle = Math.toRadians((i * 36 - 90).toDouble())
                        val x = cx + (r * kotlin.math.cos(angle)).toFloat()
                        val y = cy + (r * kotlin.math.sin(angle)).toFloat()
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    p.close()
                    drawPath(p, color = strokeColor, style = Stroke(2.5f))
                }
            }
        }
        Text(
            text = when (maskType) {
                MaskType.NONE -> "None"
                MaskType.CIRCLE -> "Circle"
                MaskType.ROUNDED_RECT -> "Rectangle"
                MaskType.SPLIT_WIPE -> "Split"
                MaskType.ANAMORPHIC_BARS -> "Filmstrip"
                MaskType.MIRROR -> "Mirror"
                MaskType.HEART -> "Heart"
                MaskType.STAR -> "Star"
            },
            fontSize = 10.sp,
            color = if (selected) Color.White else TextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun TransitionsAndEffectsSheet(
    clip: VideoClip,
    onUpdateClip: (VideoClip) -> Unit
) {
    val context = LocalContext.current
    val isImgClip = remember(clip.mediaUri, clip.isAiGeneratedBroll) {
        clip.isAiGeneratedBroll ||
            clip.mediaUri.endsWith(".png", true) ||
            clip.mediaUri.endsWith(".jpg", true) ||
            clip.mediaUri.endsWith(".jpeg", true) ||
            clip.mediaUri.endsWith(".webp", true)
    }
    val clipThumb by produceState<Bitmap?>(initialValue = null, key1 = clip.mediaUri, key2 = clip.trimStartMs) {
        if (clip.mediaUri.isNotBlank() && !isImgClip) {
            value = withContext(Dispatchers.IO) {
                val r = MediaMetadataRetriever()
                try {
                    val u = Uri.parse(clip.mediaUri)
                    if (u.scheme.isNullOrEmpty()) r.setDataSource(clip.mediaUri) else r.setDataSource(context, u)
                    val us = clip.trimStartMs.coerceAtLeast(0L) * 1000L
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 128, 96)
                    } else {
                        r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                            Bitmap.createScaledBitmap(it, 128, 96, true).also { s -> if (s !== it) it.recycle() }
                        }
                    }
                } catch (_: Throwable) {
                    null
                } finally {
                    runCatching { r.release() }
                }
            }
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Animations", "Video FX", "Transitions", "Filters")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Transitions, Video FX & Filters", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF141820),
            contentColor = ElectricCyan
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                var animGroup by remember { mutableStateOf("In") }
                var animQuery by remember { mutableStateOf("") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("In", "Out", "Combo").forEach { grp ->
                        FilterChip(
                            selected = animGroup == grp,
                            onClick = { animGroup = grp },
                            label = { Text(grp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = animQuery,
                    onValueChange = { animQuery = it },
                    placeholder = { Text("Search $animGroup Animations...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SliderRow("Anim Duration (s)", clip.animDurationMs / 1000f, 0.2f..3.0f) {
                    onUpdateClip(clip.copy(animDurationMs = (it * 1000f).toLong()))
                }
                SliderRow("Anim Intensity (10x)", clip.animIntensity, 0.2f..10.0f) {
                    onUpdateClip(clip.copy(animIntensity = it))
                }
                SliderRow("Anim Speed (10x)", clip.animSpeed, 0.25f..10.0f) {
                    onUpdateClip(clip.copy(animSpeed = it))
                }
                val filteredAnims = remember(animGroup, animQuery) {
                    EffectCatalog.allAnimations.filter { a ->
                        (a.basePreset == ClipAnimPreset.NONE || a.group == animGroup) &&
                            (animQuery.isBlank() || a.name.contains(animQuery, ignoreCase = true) || a.category.contains(animQuery, ignoreCase = true))
                    }
                }
                val activeSelected = when (animGroup) {
                    "In" -> clip.animIn
                    "Out" -> clip.animOut
                    else -> clip.animCombo
                }
                Text("${filteredAnims.size} Animations", fontSize = 10.sp, color = ElectricCyan)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    itemsIndexed(filteredAnims, key = { idx, it -> "a_${idx}_${it.name}" }) { _, anim ->
                                val isSel = (clip.animLabel.isNotBlank() && clip.animLabel == anim.name) ||
                                    (clip.animLabel.isBlank() && activeSelected == anim.basePreset)
                                Surface(
                                    onClick = {
                                        val updated = when (animGroup) {
                                            "In" -> clip.copy(animIn = anim.basePreset, animIntensity = anim.defaultIntensity, animSpeed = anim.defaultSpeed, animLabel = anim.name)
                                            "Out" -> clip.copy(animOut = anim.basePreset, animIntensity = anim.defaultIntensity, animSpeed = anim.defaultSpeed, animLabel = anim.name)
                                            else -> clip.copy(animCombo = anim.basePreset, animIntensity = anim.defaultIntensity, animSpeed = anim.defaultSpeed, animLabel = anim.name)
                                        }
                                        onUpdateClip(updated)
                                    },
                                    color = if (isSel) Color(0xFF0E2936) else SlateVoid,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) ElectricCyan else SlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CapCutPreviewThumbBox(
                                            thumb = clipThumb,
                                            mediaUri = clip.mediaUri,
                                            isImage = isImgClip,
                                            tint = when (animGroup) {
                                                "In" -> ElectricCyan.copy(alpha = 0.22f)
                                                "Out" -> WarmAmber.copy(alpha = 0.22f)
                                                else -> VioletClip.copy(alpha = 0.22f)
                                            },
                                            badge = animGroup
                                        )
                                        Text(
                                            text = anim.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) ElectricCyan else TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = anim.category,
                                            fontSize = 8.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                    }
                }
            }
            2 -> {
                Text("Clip Out Transition", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransitionType.entries.forEach { t ->
                        FilterChip(
                            selected = clip.transitionOut == t,
                            onClick = { onUpdateClip(clip.copy(transitionOut = t)) },
                            label = { Text(t.label) }
                        )
                    }
                }
                SliderRow("Duration (s)", clip.transitionDurationMs / 1000f, 0.2f..1.5f) {
                    onUpdateClip(clip.copy(transitionDurationMs = (it * 1000f).toLong()))
                }
            }
            1 -> {
                var fxQuery by remember { mutableStateOf("") }
                var fxCat by remember { mutableStateOf("All") }
                val categories = EffectCatalog.fxCategories
                OutlinedTextField(
                    value = fxQuery,
                    onValueChange = { fxQuery = it },
                    placeholder = { Text("Search 7,500+ Video Effects...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (clip.videoFx != VideoFxPreset.NONE) {
                    val clipSec = (clip.activeDurationMs / 1000f).coerceAtLeast(0.5f)
                    val endSec = if (clip.videoFxEndMs < 0L) clipSec else (clip.videoFxEndMs / 1000f).coerceIn(0.2f, clipSec)
                    SliderRow("FX Intensity (10x)", clip.videoFxIntensity, 0.1f..10.0f) {
                        onUpdateClip(clip.copy(videoFxIntensity = it))
                    }
                    SliderRow("FX Texture", clip.videoFxTexture, 0f..1.0f) {
                        onUpdateClip(clip.copy(videoFxTexture = it))
                    }
                    SliderRow("FX Color", clip.videoFxColorShift, -1.0f..1.0f) {
                        onUpdateClip(clip.copy(videoFxColorShift = it))
                    }
                    SliderRow("FX Speed (10x)", clip.videoFxSpeed, 0.25f..10.0f) {
                        onUpdateClip(clip.copy(videoFxSpeed = it))
                    }
                    SliderRow("FX Start (s)", (clip.videoFxStartMs / 1000f).coerceIn(0f, clipSec), 0f..clipSec) {
                        onUpdateClip(clip.copy(videoFxStartMs = (it * 1000f).toLong()))
                    }
                    SliderRow("FX End (s)", endSec, 0.2f..clipSec) {
                        onUpdateClip(clip.copy(videoFxEndMs = (it * 1000f).toLong()))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = fxCat == cat,
                            onClick = { fxCat = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
                val filteredFx = remember(fxCat, fxQuery) {
                    EffectCatalog.allVideoFx.filter { fx ->
                        (fxCat == "All" || fx.category == fxCat || fx.basePreset == VideoFxPreset.NONE) &&
                            (fxQuery.isBlank() || fx.name.contains(fxQuery, ignoreCase = true) || fx.category.contains(fxQuery, ignoreCase = true))
                    }
                }
                Text("${filteredFx.size} Effects", fontSize = 10.sp, color = ElectricCyan)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    itemsIndexed(filteredFx, key = { idx, it -> "f_${idx}_${it.name}" }) { _, fx ->
                                val isSel = (clip.videoFxLabel.isNotBlank() && clip.videoFxLabel == fx.name) ||
                                    (clip.videoFxLabel.isBlank() && clip.videoFx == fx.basePreset)
                                Surface(
                                    onClick = {
                                        onUpdateClip(
                                            clip.copy(
                                                videoFx = fx.basePreset,
                                                videoFxIntensity = fx.defaultIntensity,
                                                videoFxTexture = fx.defaultTexture,
                                                videoFxColorShift = fx.defaultColorShift,
                                                videoFxSpeed = fx.defaultSpeed,
                                                videoFxStartMs = 0L,
                                                videoFxEndMs = -1L,
                                                videoFxLabel = fx.name
                                            )
                                        )
                                    },
                                    color = if (isSel) Color(0xFF0E2936) else SlateVoid,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) ElectricCyan else SlateBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CapCutPreviewThumbBox(
                                            thumb = clipThumb,
                                            mediaUri = clip.mediaUri,
                                            isImage = isImgClip,
                                            tint = when {
                                                "Retro" in fx.category -> Color(0x44D97706)
                                                "Cyber" in fx.category || "Glitch" in fx.category -> Color(0x4406B6D4)
                                                "Cinema" in fx.category -> Color(0x441E293B)
                                                "Light" in fx.category -> Color(0x44FBBF24)
                                                else -> VioletClip.copy(alpha = 0.18f)
                                            },
                                            badge = fx.category.take(5)
                                        )
                                        Text(
                                            text = fx.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) ElectricCyan else TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = fx.category,
                                            fontSize = 8.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                    }
                }
            }
            else -> {
                Text("1-Tap CapCut Color Filters", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                SliderRow("Filter Intensity", clip.filterIntensity, 0f..1.5f) {
                    onUpdateClip(clip.copy(filterIntensity = it))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Original" to ColorGradeSpec(lutName = "Original"),
                        "Clear" to ColorGradeSpec(contrast = 1.15f, saturation = 1.10f, highlights = 0.12f, lutName = "Clear"),
                        "Warm" to ColorGradeSpec(warmth = 0.24f, saturation = 1.15f, contrast = 1.10f, lutName = "Warm"),
                        "Teal" to ColorGradeSpec(lift = ColorWheelPoint(-0.18f, 0.05f), gain = ColorWheelPoint(0.20f, 0.08f), contrast = 1.18f, lutName = "Teal"),
                        "Vintage" to ColorGradeSpec(contrast = 1.12f, saturation = 0.85f, warmth = 0.20f, vignette = 0.35f, lutName = "Vintage"),
                        "Moody" to ColorGradeSpec(contrast = 1.28f, saturation = 0.72f, shadows = -0.22f, vignette = 0.40f, lutName = "Moody"),
                        "Noir" to ColorGradeSpec(contrast = 1.38f, saturation = 0.0f, vignette = 0.42f, lutName = "Noir")
                    ).forEach { (name, spec) ->
                        FilterChip(
                            selected = clip.colorGrade.lutName == name,
                            onClick = { onUpdateClip(clip.copy(colorGrade = spec)) },
                            label = { Text(name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FaceSwapSheet(
    faceSwap: FaceSwapSpec,
    onPickFacePhoto: () -> Unit,
    onUpdateFaceSwap: (FaceSwapSpec) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("On-Device ML Kit Face Swap", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            text = "Pick any face photo and ML Kit will track and blend it live onto the face in your video.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Button(onClick = onPickFacePhoto, modifier = Modifier.fillMaxWidth()) {
            Text(if (faceSwap.sourceFaceUri.isBlank()) "Choose Source Face Photo" else "Change Source Face Photo")
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Live Face Swap", color = ElectricCyan, modifier = Modifier.weight(1f))
            Switch(
                checked = faceSwap.enabled,
                onCheckedChange = { onUpdateFaceSwap(faceSwap.copy(enabled = it)) }
            )
        }

        SliderRow("Face Size", faceSwap.scaleMultiplier, 0.7f..1.8f) {
            onUpdateFaceSwap(faceSwap.copy(scaleMultiplier = it))
        }
        SliderRow("Blend Opacity", faceSwap.blendOpacity, 0.3f..1f) {
            onUpdateFaceSwap(faceSwap.copy(blendOpacity = it))
        }
    }
}

@Composable
private fun CapCutPreviewThumbBox(
    thumb: Bitmap?,
    mediaUri: String,
    isImage: Boolean,
    tint: Color,
    badge: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151924)),
        contentAlignment = Alignment.Center
    ) {
        if (thumb != null) {
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isImage && mediaUri.isNotBlank()) {
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint)
        )
        Text(
            text = badge.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
