package com.aistudio.kbeditor.r7v4qx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aistudio.kbeditor.r7v4qx.ai.ViralExportKit
import com.aistudio.kbeditor.r7v4qx.model.*
import com.aistudio.kbeditor.r7v4qx.ui.theme.*

@Composable
fun ExportProSheet(
    settings: ExportSettings,
    exportProgress: Float?,
    lastExportPath: String?,
    viralKit: ViralExportKit? = null,
    onUpdateSettings: (ExportSettings) -> Unit,
    onStartExport: () -> Unit,
    onGenerateViralKit: () -> Unit = {},
    onCopyViralText: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Zero-Watermark Master Export", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        Text("Resolution", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportResolution.entries.forEach { res ->
                FilterChip(
                    selected = settings.resolution == res,
                    onClick = { onUpdateSettings(settings.copy(resolution = res)) },
                    label = { Text(res.label) }
                )
            }
        }

        Text("Frame Rate", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportFps.entries.forEach { fps ->
                FilterChip(
                    selected = settings.fps == fps,
                    onClick = { onUpdateSettings(settings.copy(fps = fps)) },
                    label = { Text(fps.label) }
                )
            }
        }

        Text("Dynamic Range & Codec", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportDynamicRange.entries.forEach { dr ->
                FilterChip(
                    selected = settings.dynamicRange == dr,
                    onClick = { onUpdateSettings(settings.copy(dynamicRange = dr)) },
                    label = { Text(dr.label) }
                )
            }
        }

        if (exportProgress != null) {
            LinearProgressIndicator(
                progress = { exportProgress },
                color = ElectricCyan,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (lastExportPath != null) {
            Text("Saved: $lastExportPath", style = MaterialTheme.typography.labelSmall, color = EmeraldTrack)
        }

        Button(
            onClick = onStartExport,
            enabled = exportProgress == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (exportProgress == null) "Export to Movies/KB Editor" else "Rendering Master...")
        }

        HorizontalDivider()
        Text("AI Viral Title, Hashtags & HD Thumbnail", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
        OutlinedButton(onClick = onGenerateViralKit, modifier = Modifier.fillMaxWidth()) {
            Text("Generate Viral Title, Hashtags & Thumbnail")
        }
        if (viralKit != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateElevated, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = viralKit.thumbnailUri,
                    contentDescription = "AI Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 64.dp, height = 104.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(viralKit.viralTitle, fontSize = 13.sp, color = TextPrimary)
                    Text(viralKit.caption, fontSize = 11.sp, color = TextMuted)
                    Text(viralKit.hashtags, fontSize = 10.sp, color = ElectricCyan)
                    TextButton(
                        onClick = { onCopyViralText("${viralKit.viralTitle}\n\n${viralKit.caption}\n\n${viralKit.hashtags}") },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Copy Title & Hashtags", fontSize = 11.sp, color = WarmAmber)
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasReframeSheet(
    project: KbEditorProject,
    onUpdateCanvas: (AspectRatioPreset, CanvasFillMode, Float) -> Unit,
    onSmartAutoReframe: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Universal Canvas & Smart Auto-Reframe", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AspectRatioPreset.entries.forEach { ratio ->
                FilterChip(
                    selected = project.aspectRatio == ratio,
                    onClick = { onUpdateCanvas(ratio, project.canvasFillMode, project.canvasBlurRadius) },
                    label = { Text(ratio.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CanvasFillMode.entries.forEach { mode ->
                FilterChip(
                    selected = project.canvasFillMode == mode,
                    onClick = { onUpdateCanvas(project.aspectRatio, mode, project.canvasBlurRadius) },
                    label = { Text(mode.label) }
                )
            }
        }

        if (project.canvasFillMode == CanvasFillMode.BLUR_CANVAS) {
            SliderRow("Canvas Blur", project.canvasBlurRadius, 4f..40f) {
                onUpdateCanvas(project.aspectRatio, project.canvasFillMode, it)
            }
        }

        OutlinedButton(onClick = onSmartAutoReframe, modifier = Modifier.fillMaxWidth()) {
            Text("Run Face & Saliency Smart Auto-Reframe")
        }
    }
}

@Composable
fun AiSidePanel(
    statusBanner: String?,
    onRunCopilot: (String) -> Unit,
    onOpenAutoCreator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var prompt by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SlateBase, RoundedCornerShape(16.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("KB AI Studio Copilot", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
        Text(
            text = statusBanner ?: "Ready to grade, speed-ramp, sync beats, or generate B-Roll.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("e.g., Grade Teal & Orange + Flash In") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (prompt.isNotBlank()) {
                    onRunCopilot(prompt)
                    prompt = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Execute AI Command")
        }

        OutlinedButton(onClick = onOpenAutoCreator, modifier = Modifier.fillMaxWidth()) {
            Text("Open Auto-Creator & B-Roll")
        }
    }
}
