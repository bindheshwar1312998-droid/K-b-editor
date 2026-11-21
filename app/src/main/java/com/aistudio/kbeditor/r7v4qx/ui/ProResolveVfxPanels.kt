package com.aistudio.kbeditor.r7v4qx.ui

import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aistudio.kbeditor.r7v4qx.model.ColorGradeSpec
import com.aistudio.kbeditor.r7v4qx.model.ColorWheelPoint
import com.aistudio.kbeditor.r7v4qx.model.KeyframePoint
import com.aistudio.kbeditor.r7v4qx.model.SpeedRampPreset
import com.aistudio.kbeditor.r7v4qx.ui.theme.*
import com.aistudio.kbeditor.r7v4qx.vfx.CurveEvaluators
import kotlin.math.hypot

@Composable
fun ColorWheelsPanel(
    grade: ColorGradeSpec,
    lastCorrectionReport: String? = null,
    onUpdateGrade: (ColorGradeSpec) -> Unit,
    onAiColorGrade: (String) -> Unit,
    onAutoColorCorrect: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("DaVinci 3-Way Color & Scopes (${grade.lutName})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        // DaVinci-style Live RGB Parade Scope
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF07090D), RoundedCornerShape(8.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            val rShift = (grade.lift.dx + grade.gamma.dx + grade.gain.dx + grade.warmth) * 0.2f
            val gShift = (grade.lift.dy + grade.gamma.dy + grade.gain.dy) * 0.2f
            val bShift = -rShift
            val channels = listOf(Color(0xFFEF4444) to rShift, Color(0xFF22C55E) to gShift, Color(0xFF3B82F6) to bShift)
            val laneW = size.width / 3f
            channels.forEachIndexed { idx, (col, shift) ->
                val p = Path()
                val startX = idx * laneW
                for (i in 0..16) {
                    val t = i / 16f
                    val x = startX + t * (laneW - 6f)
                    val curve = (0.5f + shift + kotlin.math.sin(t * 3.14f) * 0.25f * grade.contrast).coerceIn(0.05f, 0.95f)
                    val y = size.height * (1f - curve)
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                drawPath(p, color = col, style = Stroke(2.5f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAutoColorCorrect, modifier = Modifier.weight(1f)) {
                Text("1-Tap Auto Correction")
            }
            OutlinedButton(onClick = { onAiColorGrade("iPhone Cinema Warm") }, modifier = Modifier.weight(1f)) {
                Text("AI Auto Grade")
            }
        }

        if (!lastCorrectionReport.isNullOrBlank()) {
            Surface(
                color = Color(0xFF0E1726),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "AI Response: $lastCorrectionReport",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // 1-Tap Pro Film LUTs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Rec.709" to ColorGradeSpec(lutName = "Rec.709"),
                "Kodak 2383" to ColorGradeSpec(contrast = 1.22f, saturation = 1.16f, warmth = 0.14f, vignette = 0.28f, shadows = -0.10f, lutName = "Kodak 2383"),
                "iPhone Cinema" to ColorGradeSpec(contrast = 1.14f, saturation = 1.12f, warmth = 0.08f, highlights = 0.12f, beautyGlow = 0.25f, vignette = 0.18f, lutName = "iPhone Cinema"),
                "Teal & Orange" to ColorGradeSpec(lift = ColorWheelPoint(-0.20f, 0.05f), gain = ColorWheelPoint(0.24f, 0.08f), contrast = 1.20f, saturation = 1.18f, vignette = 0.25f, lutName = "Teal & Orange"),
                "Cyber Neon" to ColorGradeSpec(lift = ColorWheelPoint(0.18f, -0.15f), gain = ColorWheelPoint(-0.15f, 0.20f), contrast = 1.26f, saturation = 1.35f, beautyGlow = 0.35f, lutName = "Cyber Neon"),
                "B&W Noir" to ColorGradeSpec(contrast = 1.36f, saturation = 0.0f, shadows = -0.18f, vignette = 0.42f, lutName = "B&W Noir")
            ).forEach { (name, spec) ->
                FilterChip(
                    selected = grade.lutName == name,
                    onClick = { onUpdateGrade(spec) },
                    label = { Text(name) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleColorWheel("Lift (Shadows)", grade.lift) { pt ->
                onUpdateGrade(grade.copy(lift = pt))
            }
            SingleColorWheel("Gamma (Mid)", grade.gamma) { pt ->
                onUpdateGrade(grade.copy(gamma = pt))
            }
            SingleColorWheel("Gain (High)", grade.gain) { pt ->
                onUpdateGrade(grade.copy(gain = pt))
            }
        }

        SliderRow("Contrast", grade.contrast, 0.5f..1.8f) { onUpdateGrade(grade.copy(contrast = it)) }
        SliderRow("Saturation", grade.saturation, 0f..2f) { onUpdateGrade(grade.copy(saturation = it)) }
        SliderRow("Vibrance (Skin-Safe)", grade.vibrance, -0.6f..1.0f) { onUpdateGrade(grade.copy(vibrance = it)) }
        SliderRow("Warmth", grade.warmth, -0.8f..0.8f) { onUpdateGrade(grade.copy(warmth = it)) }
        SliderRow("Highlights", grade.highlights, -0.6f..0.6f) { onUpdateGrade(grade.copy(highlights = it)) }
        SliderRow("Shadows", grade.shadows, -0.6f..0.6f) { onUpdateGrade(grade.copy(shadows = it)) }
        SliderRow("Vignette", grade.vignette, 0f..1f) { onUpdateGrade(grade.copy(vignette = it)) }
        SliderRow("Film Glow", grade.beautyGlow, 0f..1f) { onUpdateGrade(grade.copy(beautyGlow = it)) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAiColorGrade("Teal & Orange Cinema") }) {
                Text("AI Grade")
            }
            OutlinedButton(onClick = { onUpdateGrade(ColorGradeSpec()) }) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun SingleColorWheel(
    label: String,
    point: ColorWheelPoint,
    onChange: (ColorWheelPoint) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(102.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .border(1.5.dp, SlateBorder, CircleShape)
                .pointerInput(point) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val r = size.width / 2f
                        val dx = ((change.position.x - r) / r).coerceIn(-1f, 1f)
                        val dy = ((change.position.y - r) / r).coerceIn(-1f, 1f)
                        val dist = hypot(dx, dy)
                        val nx = if (dist > 1f) dx / dist else dx
                        val ny = if (dist > 1f) dy / dist else dy
                        onChange(point.copy(dx = nx, dy = ny))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    ),
                    alpha = 0.72f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
                val px = size.width / 2f + point.dx * (size.width / 2f - 8f)
                val py = size.height / 2f + point.dy * (size.height / 2f - 8f)
                drawCircle(Color.White, radius = 6f, center = Offset(px, py))
                drawCircle(SlateVoid, radius = 6f, center = Offset(px, py), style = Stroke(2f))
            }
        }
        Slider(
            value = point.luma,
            onValueChange = { onChange(point.copy(luma = it)) },
            valueRange = -0.5f..0.5f,
            modifier = Modifier.height(22.dp)
        )
    }
}

@Composable
fun SpeedRampGraph(preset: SpeedRampPreset) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .background(Color(0xFF0A0C10), RoundedCornerShape(10.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // CapCut-style horizontal reference grid lines (0.2x, 1.0x, 2.5x)
            val gridColor = Color.White.copy(alpha = 0.10f)
            for (g in 0..3) {
                val gy = size.height * (g / 3f)
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            // 1.0x baseline
            val base1xY = size.height - ((1.0f - 0.25f) / 2.75f) * size.height
            drawLine(WarmAmber.copy(alpha = 0.45f), Offset(0f, base1xY), Offset(size.width, base1xY), strokeWidth = 1.5f)

            val path = Path()
            val fillPath = Path()
            val steps = 40
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val speed = CurveEvaluators.evaluateSpeedMultiplier(preset, t)
                val x = t * size.width
                val y = size.height - ((speed - 0.25f) / 2.75f).coerceIn(0f, 1f) * size.height
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(ElectricCyan.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(path, color = ElectricCyan, style = Stroke(width = 4f))

            // CapCut-style 5 control nodes along the curve
            for (k in 0..4) {
                val kt = k / 4f
                val ks = CurveEvaluators.evaluateSpeedMultiplier(preset, kt)
                val kx = kt * size.width
                val ky = size.height - ((ks - 0.25f) / 2.75f).coerceIn(0f, 1f) * size.height
                drawCircle(Color.White, radius = 6.5f, center = Offset(kx, ky))
                drawCircle(ElectricCyan, radius = 6.5f, center = Offset(kx, ky), style = Stroke(2.5f))
            }
        }
    }
}

@Composable
fun KeyframeGraphSheet(
    keyframes: List<KeyframePoint>,
    currentNormT: Float,
    onToggleKeyframe: () -> Unit,
    onUpdateKeyframe: (scale: Float, rotationDeg: Float, x: Float, y: Float, opacity: Float) -> Unit,
    onClearAll: () -> Unit,
    onApplyPreset: (String) -> Unit = {},
    onSelectCurve: (String) -> Unit = {}
) {
    val activeKf = remember(keyframes, currentNormT) {
        CurveEvaluators.interpolateKeyframes(keyframes, currentNormT)
    }
    var scale by remember(currentNormT, keyframes.size) { mutableFloatStateOf(activeKf.scale) }
    var rotation by remember(currentNormT, keyframes.size) { mutableFloatStateOf(activeKf.rotationDeg) }
    var posX by remember(currentNormT, keyframes.size) { mutableFloatStateOf(activeKf.x) }
    var posY by remember(currentNormT, keyframes.size) { mutableFloatStateOf(activeKf.y) }
    var opacity by remember(currentNormT, keyframes.size) { mutableFloatStateOf(activeKf.opacity) }
    var selectedGraphCurve by remember(keyframes) { mutableStateOf(keyframes.firstOrNull()?.easingCurve ?: "Ease In-Out") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keyframe & Motion Graph (${keyframes.size} ◆)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            FilledTonalButton(onClick = onToggleKeyframe) {
                Text("◆ Add/Remove", color = WarmAmber)
            }
        }

        // CapCut-style Easing Graph Preview
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(Color(0xFF0A0C10), RoundedCornerShape(10.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            val grid = Color.White.copy(alpha = 0.09f)
            drawLine(grid, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1f)
            drawLine(grid, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1f)

            val p = Path().apply {
                moveTo(0f, size.height)
                when (selectedGraphCurve) {
                    "Linear" -> lineTo(size.width, 0f)
                    "Ease In" -> cubicTo(size.width * 0.65f, size.height, size.width * 0.9f, size.height * 0.4f, size.width, 0f)
                    "Ease Out" -> cubicTo(size.width * 0.1f, size.height * 0.6f, size.width * 0.35f, 0f, size.width, 0f)
                    "Custom Bezier" -> cubicTo(size.width * 0.18f, size.height * 0.95f, size.width * 0.82f, size.height * 0.05f, size.width, 0f)
                    else -> cubicTo(size.width * 0.42f, size.height, size.width * 0.58f, 0f, size.width, 0f)
                }
            }
            drawPath(p, color = WarmAmber, style = Stroke(4f))

            // Live playhead indicator on graph
            val px = currentNormT.coerceIn(0f, 1f) * size.width
            drawLine(ElectricCyan.copy(alpha = 0.7f), Offset(px, 0f), Offset(px, size.height), strokeWidth = 2f)
            // Draw keyframe diamonds along the timeline axis
            keyframes.forEach { kf ->
                val dx = kf.normalizedTime * size.width
                val dy = size.height * (1f - kf.normalizedTime)
                drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(dx, dy))
                drawCircle(WarmAmber, radius = 5.dp.toPx(), center = Offset(dx, dy), style = Stroke(2.dp.toPx()))
            }
        }

        Text("1-Tap Motion Presets", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ZOOM_IN" to "Zoom In", "ZOOM_OUT" to "Zoom Out", "PAN_LR" to "Pan L→R", "SPIN_POP" to "Spin Pop").forEach { (pid, plabel) ->
                AssistChip(onClick = { onApplyPreset(pid) }, label = { Text(plabel) })
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Linear", "Ease In", "Ease Out", "Ease In-Out", "Custom Bezier", "Bounce").forEach { curve ->
                FilterChip(
                    selected = selectedGraphCurve == curve,
                    onClick = {
                        selectedGraphCurve = curve
                        onSelectCurve(curve)
                    },
                    label = { Text(curve, fontSize = 10.sp) }
                )
            }
        }

        SliderRow("Scale", scale, 0.4f..2.5f) {
            scale = it
            onUpdateKeyframe(scale, rotation, posX, posY, opacity)
        }
        SliderRow("Rotate", rotation, -180f..180f) {
            rotation = it
            onUpdateKeyframe(scale, rotation, posX, posY, opacity)
        }
        SliderRow("Pos X", posX, 0f..1f) {
            posX = it
            onUpdateKeyframe(scale, rotation, posX, posY, opacity)
        }
        SliderRow("Pos Y", posY, 0f..1f) {
            posY = it
            onUpdateKeyframe(scale, rotation, posX, posY, opacity)
        }
        SliderRow("Opacity", opacity, 0f..1f) {
            opacity = it
            onUpdateKeyframe(scale, rotation, posX, posY, opacity)
        }

        if (keyframes.isNotEmpty()) {
            OutlinedButton(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
                Text("Clear All Keyframes")
            }
        }
    }
}

@Composable
fun MotionTrackerPanel(
    pointCount: Int,
    trackMode: String,
    onModeChange: (String) -> Unit,
    onStartTapOnVideo: (String) -> Unit,
    onRunTrack: (Float, Float, String) -> Unit,
    onClearTrack: () -> Unit
) {
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var targetY by remember { mutableFloatStateOf(0.45f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("2D / 3D Motion Tracker", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            text = if (trackMode == "3D") {
                "3D Camera Follow: Locks onto subject, zooms & pans camera automatically ($pointCount pts)."
            } else {
                "2D Subject Track: Attaches captions, stickers & PiP to moving subject ($pointCount pts)."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = trackMode == "2D",
                onClick = { onModeChange("2D") },
                label = { Text("2D Subject Track") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = trackMode == "3D",
                onClick = { onModeChange("3D") },
                label = { Text("3D Camera Follow") },
                modifier = Modifier.weight(1f)
            )
        }
        Button(
            onClick = { onStartTapOnVideo(trackMode) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = SlateVoid)
        ) {
            Text("Tap Directly on Video / Photo to Lock", fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(SlateVoid, RoundedCornerShape(12.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        targetX = (offset.x / size.width).coerceIn(0.1f, 0.9f)
                        targetY = (offset.y / size.height).coerceIn(0.1f, 0.9f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width * targetX
                val cy = size.height * targetY
                drawCircle(ElectricCyan, radius = 24f, center = Offset(cx, cy), style = Stroke(2.5f))
                drawLine(ElectricCyan, Offset(cx - 32f, cy), Offset(cx + 32f, cy), strokeWidth = 2f)
                drawLine(ElectricCyan, Offset(cx, cy - 32f), Offset(cx, cy + 32f), strokeWidth = 2f)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onRunTrack(targetX, targetY, trackMode) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Lock $trackMode Target")
            }
            if (pointCount > 0) {
                OutlinedButton(onClick = onClearTrack) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(100.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f).height(24.dp)
        )
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan,
            modifier = Modifier.width(42.dp)
        )
    }
}
