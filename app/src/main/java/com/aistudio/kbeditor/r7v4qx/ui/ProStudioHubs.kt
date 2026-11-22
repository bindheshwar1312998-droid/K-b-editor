package com.aistudio.kbeditor.r7v4qx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.kbeditor.r7v4qx.audio.CapCutSfxPreset
import com.aistudio.kbeditor.r7v4qx.audio.SfxCatalog
import com.aistudio.kbeditor.r7v4qx.model.*
import com.aistudio.kbeditor.r7v4qx.ui.theme.ElectricCyan
import com.aistudio.kbeditor.r7v4qx.ui.theme.TextMuted
import com.aistudio.kbeditor.r7v4qx.ui.theme.TextPrimary

@Composable
fun PipOverlaySheet(
    pipClips: List<OverlayPipClip> = emptyList(),
    onPickPipMedia: () -> Unit,
    onAddOpticalPreset: (String, BlendModePreset) -> Unit,
    onMoveSelectedToOverlay: (BlendModePreset) -> Unit = {},
    onDuplicatePip: (String) -> Unit = {},
    onReplacePip: (String) -> Unit = {},
    onDeletePip: (String) -> Unit = {}
) {
    var selectedBlend by remember { mutableStateOf(BlendModePreset.SCREEN) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text("PiP Overlays & Blend Modes (Track O1)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("Blend Mode", style = MaterialTheme.typography.labelSmall, color = TextMuted)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BlendModePreset.entries.forEach { mode ->
                FilterChip(
                    selected = selectedBlend == mode,
                    onClick = { selectedBlend = mode },
                    label = { Text(mode.label) }
                )
            }
        }

        Button(onClick = onPickPipMedia, modifier = Modifier.fillMaxWidth()) {
            Text("Import Photo / Video PiP Overlay")
        }
        OutlinedButton(onClick = { onMoveSelectedToOverlay(selectedBlend) }, modifier = Modifier.fillMaxWidth()) {
            Text("Move Selected V1 Clip to Overlay (O1)")
        }

        Text("Optical FX Presets", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Neon Light Leak", "Anamorphic Flare", "Cyber HUD Grid").forEach { preset ->
                OutlinedButton(onClick = { onAddOpticalPreset(preset, selectedBlend) }) {
                    Text(preset)
                }
            }
        }

        if (pipClips.isNotEmpty()) {
            HorizontalDivider()
            Text("Active Overlays (${pipClips.size}) — Duplicate / Replace", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            pipClips.forEach { pip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF151923))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(pip.title, fontSize = 11.sp, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onDuplicatePip(pip.id) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Dup", fontSize = 10.sp, color = ElectricCyan)
                    }
                    TextButton(onClick = { onReplacePip(pip.id) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Replace", fontSize = 10.sp, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
                    }
                    IconButton(onClick = { onDeletePip(pip.id) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun Caption3DSheet(
    captions: List<CaptionClip>,
    dailyCoins: Int,
    onAddCaption: (String, MotionAnimation3D, CaptionStylePreset, Long, Long, Float, Float, CaptionFontStyle) -> Unit,
    onRunAutoCaptions: (CaptionLanguage, CaptionStylePreset, Long, Long, Float, Float, String) -> Unit,
    onApplyDesignToAll: (CaptionStylePreset, MotionAnimation3D, Long, Long, Float, Float, CaptionFontStyle) -> Unit,
    onDeleteCaption: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) }
    var selectedLang by remember { mutableStateOf(CaptionLanguage.AUTO_DETECT) }
    var selectedStyle by remember { mutableStateOf(CaptionStylePreset.VIRAL_HORMOZI) }
    var selectedFont by remember { mutableStateOf(CaptionFontStyle.SANS_BOLD) }
    var anim by remember { mutableStateOf(MotionAnimation3D.POP_3D) }
    var text by remember { mutableStateOf("SHOT ON KB STUDIO") }
    var fontSizeSp by remember { mutableStateOf(22f) }
    var posY by remember { mutableStateOf(0.8f) }
    var colorHex by remember { mutableStateOf(0xFFF8FAFC) }
    var accentHex by remember { mutableStateOf(0xFFF59E0B) }

    val colorChoices = listOf(0xFFF8FAFC, 0xFFF59E0B, 0xFF22D3EE, 0xFFEC4899, 0xFF10B981, 0xFFA855F7)

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
            Text("Auto Captions & Design (T1)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("Pro: $dailyCoins C", style = MaterialTheme.typography.labelMedium, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                label = { Text("AI Auto-Captions (All Langs)") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                label = { Text("Manual & Edit (${captions.size})") },
                modifier = Modifier.weight(1f)
            )
        }

        // Caption Style Design Cards (shared across AI + Manual)
        Text("Caption Design Style", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CaptionStylePreset.entries.forEach { preset ->
                val isSel = selectedStyle == preset
                FilterChip(
                    selected = isSel,
                    onClick = { selectedStyle = preset },
                    label = { Text(preset.label, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Text("Font Family (CapCut Pro)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CaptionFontStyle.entries.forEach { f ->
                FilterChip(
                    selected = selectedFont == f,
                    onClick = { selectedFont = f },
                    label = { Text(f.label, fontSize = 11.sp) }
                )
            }
        }

        // Color & Accent Swatches
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Color:", fontSize = 11.sp, color = TextMuted)
            colorChoices.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(if (accentHex == hex) 2.5.dp else 1.dp, if (accentHex == hex) Color.White else Color.DarkGray, CircleShape)
                        .clickable {
                            accentHex = hex
                            colorHex = if (hex == 0xFFF8FAFC) 0xFFF8FAFC else hex
                        }
                )
            }
        }

        // Size & Vertical Position Sliders
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Size: ${fontSizeSp.toInt()}sp", fontSize = 11.sp, color = TextMuted)
                Slider(value = fontSizeSp, onValueChange = { fontSizeSp = it }, valueRange = 14f..36f)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Position Y: ${(posY * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                Slider(value = posY, onValueChange = { posY = it }, valueRange = 0.15f..0.90f)
            }
        }

        if (tabIndex == 0) {
            var customLang by remember { mutableStateOf("") }
            Text("22 Official + Regional Boliyan + Any Language", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CaptionLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = selectedLang == lang && customLang.isBlank(),
                        onClick = {
                            selectedLang = lang
                            customLang = ""
                        },
                        label = { Text(lang.label, fontSize = 11.sp) }
                    )
                }
            }
            OutlinedTextField(
                value = customLang,
                onValueChange = { customLang = it },
                label = { Text("Or Type Any Boli / Language (e.g. Braj, Nagpuri, Kangri)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onRunAutoCaptions(selectedLang, selectedStyle, colorHex, accentHex, fontSizeSp, posY, customLang)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Generate ${customLang.ifBlank { selectedLang.label }} Auto-Captions (5C)")
            }

            if (captions.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onApplyDesignToAll(selectedStyle, anim, colorHex, accentHex, fontSizeSp, posY, selectedFont) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Design to All ${captions.size} Captions")
                }
            }
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Caption Text (Any Language)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MotionAnimation3D.entries.forEach { preset ->
                    FilterChip(
                        selected = anim == preset,
                        onClick = { anim = preset },
                        label = { Text(preset.label) }
                    )
                }
            }
            Button(
                onClick = { onAddCaption(text, anim, selectedStyle, colorHex, accentHex, fontSizeSp, posY, selectedFont) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Styled Caption at Playhead")
            }

            if (captions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Captions (${captions.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber, fontSize = 11.sp)
                    }
                }
                captions.take(8).forEach { cap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF151923))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cap.text, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                            Text("${cap.startMs}ms • ${cap.stylePreset.label}", fontSize = 10.sp, color = TextMuted)
                        }
                        IconButton(onClick = { onDeleteCaption(cap.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioProSheet(
    audioClip: AudioClip?,
    audioClips: List<AudioClip> = emptyList(),
    onExtractAudio: () -> Unit,
    onUpdateDsp: (Boolean, Boolean, Boolean, VocalIsolationMode) -> Unit,
    onUpdateEchoRemoval: (Boolean) -> Unit = {},
    onDetectBeats: () -> Unit,
    onGenerateVoiceover: (String, String) -> Unit,
    onAddSfx: (CapCutSfxPreset) -> Unit = {},
    onAddSfxVariant: (String, CapCutSfxPreset, Float, Float) -> Unit = { _, p, _, _ -> onAddSfx(p) },
    onDeleteAudioClip: (String) -> Unit = {},
    onImportOnlineAudio: (String) -> Unit = {},
    onPickLocalAudio: () -> Unit = {},
    isRecordingMic: Boolean = false,
    onToggleMicRecord: () -> Unit = {},
    onAutoBeatCut: () -> Unit = {},
    denoiseBackendLabel: String = "DSP Fallback (No AI model bundled)",
    abBypassNoiseReduction: Boolean = false,
    onToggleAbBypass: (Boolean) -> Unit = {},
    onUpdateNoiseStrength: (Float) -> Unit = {},
    onRunOfflineDenoise: (Float, Boolean) -> Unit = { _, _ -> }
) {
    var wind by remember(audioClip?.windFilterEnabled) { mutableStateOf(audioClip?.windFilterEnabled ?: false) }
    var gate by remember(audioClip?.noiseGateEnabled) { mutableStateOf(audioClip?.noiseGateEnabled ?: false) }
    var voiceEq by remember(audioClip?.voiceEnhanceEnabled) { mutableStateOf(audioClip?.voiceEnhanceEnabled ?: false) }
    var vocalMode by remember(audioClip?.vocalMode) { mutableStateOf(audioClip?.vocalMode ?: VocalIsolationMode.FULL_MIX) }
    var voScript by remember { mutableStateOf("") }
    var voLang by remember { mutableStateOf("en-US") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Audio Pro & AI DSP (Track A1)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExtractAudio, modifier = Modifier.weight(1f)) {
                Text("Extract Audio")
            }
            OutlinedButton(onClick = onDetectBeats, modifier = Modifier.weight(1f)) {
                Text("Auto-Beat Sync")
            }
        }

        Button(
            onClick = onAutoBeatCut,
            colors = ButtonDefaults.buttonColors(
                containerColor = com.aistudio.kbeditor.r7v4qx.ui.theme.VioletClip,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚡ Auto Beat-Cut AI (Sync Video Cuts to Music)")
        }

        Button(
            onClick = onToggleMicRecord,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecordingMic) Color(0xFFEF4444) else Color(0xFF10B981),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRecordingMic) "⏹ Stop & Save Mic Voiceover to A1" else "🎙️ Record Manual Voiceover (Mic)")
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("High-Pass Wind Filter", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = wind, onCheckedChange = {
                wind = it
                onUpdateDsp(wind, gate, voiceEq, vocalMode)
            })
        }

        var noiseStrength by remember(audioClip?.noiseReductionStrength) { mutableStateOf(audioClip?.noiseReductionStrength ?: 0.75f) }
        var applyEntireVideo by remember { mutableStateOf(true) }

        Text("Noise Reduction Engine: $denoiseBackendLabel", fontSize = 11.sp, color = ElectricCyan)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Noise Reduction (Fan / Wind / Hum / Hiss)", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = gate, onCheckedChange = {
                gate = it
                onUpdateDsp(wind, gate, voiceEq, vocalMode)
            })
        }

        if (gate) {
            Text("Reduction Strength: ${(noiseStrength * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
            Slider(
                value = noiseStrength,
                onValueChange = {
                    noiseStrength = it
                    onUpdateNoiseStrength(it)
                },
                valueRange = 0f..1f
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !abBypassNoiseReduction,
                    onClick = { onToggleAbBypass(false) },
                    label = { Text("After (Cleaned)") }
                )
                FilterChip(
                    selected = abBypassNoiseReduction,
                    onClick = { onToggleAbBypass(true) },
                    label = { Text("Before (Original A/B)") }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Apply to Entire Video / All Audio", fontSize = 12.sp, color = TextMuted, modifier = Modifier.weight(1f))
                Switch(checked = applyEntireVideo, onCheckedChange = { applyEntireVideo = it })
            }
            OutlinedButton(
                onClick = { onRunOfflineDenoise(noiseStrength, applyEntireVideo) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Offline Denoised Track (WAV/AAC/MP3/Video)")
            }
        }

        var echoRem by remember(audioClip?.echoRemovalEnabled) { mutableStateOf(audioClip?.echoRemovalEnabled ?: false) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Room Echo / Reverb Removal", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = echoRem, onCheckedChange = {
                echoRem = it
                onUpdateEchoRemoval(it)
            })
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Podcast Voice Clarity EQ", color = ElectricCyan, modifier = Modifier.weight(1f))
            Switch(checked = voiceEq, onCheckedChange = {
                voiceEq = it
                onUpdateDsp(wind, gate, voiceEq, vocalMode)
            })
        }

        Text("Mid/Side Vocal Isolator", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VocalIsolationMode.entries.forEach { mode ->
                FilterChip(
                    selected = vocalMode == mode,
                    onClick = {
                        vocalMode = mode
                        onUpdateDsp(wind, gate, voiceEq, vocalMode)
                    },
                    label = { Text(mode.label) }
                )
            }
        }

        HorizontalDivider()
        Text("Multilingual AI Voiceover (TTS)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        OutlinedTextField(
            value = voScript,
            onValueChange = { voScript = it },
            label = { Text("Voiceover Script") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("en-US" to "English", "hi-IN" to "Hindi", "es-ES" to "Spanish").forEach { (tag, label) ->
                FilterChip(
                    selected = voLang == tag,
                    onClick = { voLang = tag },
                    label = { Text(label) }
                )
            }
        }
        Button(
            onClick = { onGenerateVoiceover(voScript, voLang) },
            enabled = voScript.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Synthesize Voiceover to A1")
        }

        HorizontalDivider()
        Text("All Sound FX (Action • Auction • Transition • UI)", style = MaterialTheme.typography.labelSmall, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        var sfxCat by remember { mutableStateOf("All") }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "Action", "Auction", "Transition", "Impact", "UI & Text", "Vibe").forEach { cat ->
                FilterChip(
                    selected = sfxCat == cat,
                    onClick = { sfxCat = cat },
                    label = { Text(cat, fontSize = 11.sp) }
                )
            }
        }
        val filteredSfx = remember(sfxCat) {
            SfxCatalog.allSfx.filter { sfxCat == "All" || it.preset.category == sfxCat }
        }
        Text("${filteredSfx.size} Pro SFX Ready", fontSize = 10.sp, color = ElectricCyan)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(filteredSfx, key = { idx, item -> "sfx_${idx}_${item.name}" }) { _, sfx ->
                AssistChip(
                    onClick = { onAddSfxVariant(sfx.name, sfx.preset, sfx.pitchMul, sfx.gainMul) },
                    label = { Text(sfx.name, fontSize = 11.sp) }
                )
            }
        }

        var onlineAudioUrl by remember { mutableStateOf("") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = onlineAudioUrl,
                onValueChange = { onlineAudioUrl = it },
                placeholder = { Text("Paste Online MP3/WAV SFX URL", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (onlineAudioUrl.isNotBlank()) {
                        onImportOnlineAudio(onlineAudioUrl)
                        onlineAudioUrl = ""
                    }
                }
            ) {
                Text("Add", fontSize = 11.sp)
            }
        }
        OutlinedButton(onClick = onPickLocalAudio, modifier = Modifier.fillMaxWidth()) {
            Text("Import Audio / Music from Phone")
        }

        if (audioClips.isNotEmpty()) {
            Text("Track A1 Clips (${audioClips.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            audioClips.take(6).forEach { clip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF151923))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(clip.title, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                        Text("Start: ${clip.startMs}ms • ${clip.durationMs}ms", fontSize = 10.sp, color = TextMuted)
                    }
                    IconButton(onClick = { onDeleteAudioClip(clip.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Audio", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AutoCreatorSheet(
    dailyCoins: Int,
    lastCorrectionReport: String? = null,
    onRunAutoCreator: (String) -> Unit,
    onGenerateAiBroll: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("High-energy cinematic reel with teal orange LUT and hero dip speed ramp") }
    var brollPrompt by remember { mutableStateOf("Futuristic neon city skyline at night") }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("One-Prompt AI Auto-Creator", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
            Text("Pro: $dailyCoins / 50 Coins", style = MaterialTheme.typography.labelMedium, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        }
        Text("Beginner 1-Tap Quick Prompts", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Cinematic Teal Reel + Bass Drop",
                "iPhone Warm Vlog + Clean Voice",
                "Action Glitch + Zoom Beat + Punch",
                "Cyber Neon + Hero Dip + Whoosh"
            ).forEach { qp ->
                AssistChip(
                    onClick = { prompt = qp },
                    label = { Text(qp, fontSize = 11.sp) }
                )
            }
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe your full edit in 1 line") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onRunAutoCreator(prompt) }, modifier = Modifier.fillMaxWidth()) {
            Text("Run Gemini 2.5 Flash Auto-Edit (5 Coins)")
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

        HorizontalDivider()
        Text("Direct Timeline AI Scene (V1)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        OutlinedTextField(
            value = brollPrompt,
            onValueChange = { brollPrompt = it },
            label = { Text("AI Scene / B-Roll Prompt") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onGenerateAiBroll(brollPrompt) }, modifier = Modifier.fillMaxWidth()) {
            Text("Generate AI Scene to Timeline V1 (5 Coins)")
        }
    }
}


@Composable
fun AudioDubbingSheet(
    dailyCoins: Int,
    onRunAutoDub: (CaptionLanguage, Boolean, Boolean, String) -> Unit
) {
    var selectedLang by remember { mutableStateOf(CaptionLanguage.HINDI) }
    var customLang by remember { mutableStateOf("") }
    var muteOriginal by remember { mutableStateOf(true) }
    var alsoCaptions by remember { mutableStateOf(true) }

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
            Text("Audio Dubbing (AI Voice Translate)", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
            Text("Pro: $dailyCoins C", style = MaterialTheme.typography.labelMedium, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        }
        Text(
            "Automatically translates your video's speech into any language and adds dubbed audio to Track A1.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CaptionLanguage.entries.filter { it != CaptionLanguage.AUTO_DETECT }.forEach { lang ->
                FilterChip(
                    selected = selectedLang == lang && customLang.isBlank(),
                    onClick = {
                        selectedLang = lang
                        customLang = ""
                    },
                    label = { Text(lang.label, fontSize = 11.sp) }
                )
            }
        }

        OutlinedTextField(
            value = customLang,
            onValueChange = { customLang = it },
            label = { Text("Or Type Any Dubbing Language / Boli") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Mute Original Video Voice", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = muteOriginal, onCheckedChange = { muteOriginal = it })
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Add Translated Captions (T1)", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = alsoCaptions, onCheckedChange = { alsoCaptions = it })
        }

        Button(
            onClick = { onRunAutoDub(selectedLang, muteOriginal, alsoCaptions, customLang) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Auto-Dub to ${customLang.ifBlank { selectedLang.label }} (5C)")
        }
    }
}


@Composable
fun StickersSheet(
    pipClips: List<OverlayPipClip>,
    onAddSticker: (String) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
    val stickers = listOf(
        "🔥 TRENDING", "🔔 SUBSCRIBE", "❤️ LIKE", "💥 MEGA DEAL",
        "⚡ NEW", "🎬 ACTION", "👑 PRO", "💯 100%",
        "👇 LINK BELOW", "🎉 OFFER", "🚀 VIRAL", "😂 LOL",
        "✨ WOW", "📢 ALERT"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Stickers & Emoji Badges (Track O1)", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
        Text("Tap any sticker to place at playhead (drag on video to move)", style = MaterialTheme.typography.labelSmall, color = TextMuted)

        stickers.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    OutlinedButton(
                        onClick = { onAddSticker(item) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(item, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        val activeStickers = pipClips.filter { it.opticalFxPreset?.startsWith("STICKER:") == true }
        if (activeStickers.isNotEmpty()) {
            Text("Active Stickers (${activeStickers.size})", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            activeStickers.forEach { st ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF151923))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(st.title, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteSticker(st.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun EnhancerSheet(
    audioClip: AudioClip?,
    onPickAndEnhancePhoto: () -> Unit,
    onEnhanceCurrentPhoto: () -> Unit,
    onEnhanceVideo: (String) -> Unit,
    onEnhanceVoice: (Boolean) -> Unit
) {
    var voiceEnhanced by remember { mutableStateOf(audioClip?.voiceEnhanceEnabled ?: false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pro Enhancer Studio (Photo • Video • Voice)", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)

        Text("1. Photo Enhancer (HD Upscale + Unsharp Clarity)", style = MaterialTheme.typography.labelSmall, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onPickAndEnhancePhoto, modifier = Modifier.weight(1f)) {
                Text("Pick & Enhance Photo", fontSize = 11.sp)
            }
            OutlinedButton(onClick = onEnhanceCurrentPhoto, modifier = Modifier.weight(1f)) {
                Text("Enhance Current", fontSize = 11.sp)
            }
        }

        HorizontalDivider()
        Text("2. Video Enhancer (Crispness • HDR • Skin Retouch)", style = MaterialTheme.typography.labelSmall, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onEnhanceVideo("HD_CRISP") }, modifier = Modifier.weight(1f)) {
                Text("HD Crisp", fontSize = 11.sp)
            }
            Button(onClick = { onEnhanceVideo("4K_ULTRA") }, modifier = Modifier.weight(1f)) {
                Text("4K Ultra", fontSize = 11.sp)
            }
            OutlinedButton(onClick = { onEnhanceVideo("SKIN_RETOUCH") }, modifier = Modifier.weight(1f)) {
                Text("Skin Glow", fontSize = 11.sp)
            }
        }

        HorizontalDivider()
        Text("3. Voice Enhancer (Studio Mic + Noise Removal)", style = MaterialTheme.typography.labelSmall, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("1-Tap Studio Voice Enhancer", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = voiceEnhanced,
                onCheckedChange = {
                    voiceEnhanced = it
                    onEnhanceVoice(it)
                }
            )
        }
    }
}


@Composable
fun VoiceCloneSheet(
    generatedScript: String,
    isRecordingSample: Boolean = false,
    matchedPitch: Float? = null,
    matchedSpeed: Float? = null,
    matchedLangTag: String? = null,
    voiceProfileSummary: String? = null,
    voiceSampleLabel: String? = null,
    hasVoiceSample: Boolean = false,
    onToggleVoiceMatcher: () -> Unit = {},
    onPickVoiceSample: () -> Unit = {},
    onGenerateScript: (String, String) -> Unit,
    onSynthesizeVoice: (String, String, Float, Float, String, String) -> Unit,
    onCloneFromSample: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var topic by remember { mutableStateOf("") }
    var script by remember(generatedScript) { mutableStateOf(generatedScript) }
    var langTag by remember(matchedLangTag) { mutableStateOf(matchedLangTag ?: "hi-IN") }
    var pitch by remember(matchedPitch) { mutableStateOf(matchedPitch ?: 0.95f) }
    var speed by remember(matchedSpeed) { mutableStateOf(matchedSpeed ?: 1.02f) }
    var showCloneApi by remember { mutableStateOf(false) }
    var elevenKey by remember { mutableStateOf("") }
    var elevenVoiceId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("AI Voice Studio (Smart Matcher & Clone)", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)

        Button(
            onClick = onToggleVoiceMatcher,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecordingSample) Color(0xFFEF4444) else Color(0xFF1E293B),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRecordingSample) "⏹ Stop 5s Sample & Match My Voice" else "🎙️ Smart Voice Matcher (Record 5s Sample)",
                fontSize = 11.sp
            )
        }
        if (!voiceProfileSummary.isNullOrBlank()) {
            Text(
                text = "✓ $voiceProfileSummary",
                fontSize = 11.sp,
                color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                placeholder = { Text("Topic (e.g. Salon Reel, Vlog Intro)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { onGenerateScript(topic, if (langTag == "hi-IN") "Hindi/Hinglish" else langTag) }) {
                Text("AI Script", fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = script,
            onValueChange = { script = it },
            label = { Text("Voiceover Script") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Voice Style Preset", style = MaterialTheme.typography.labelSmall, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("Deep Male", 0.78f, 0.95f),
                Triple("Crisp Male", 0.95f, 1.04f),
                Triple("Warm Female", 1.24f, 1.0f),
                Triple("Energetic Reel", 1.08f, 1.18f),
                Triple("Storyteller", 0.88f, 0.86f)
            ).forEach { (label, pVal, sVal) ->
                AssistChip(
                    onClick = { pitch = pVal; speed = sVal },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pitch: ${"%.2f".format(pitch)}x", fontSize = 11.sp, color = TextMuted)
                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.6f..1.5f)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Speed: ${"%.2f".format(speed)}x", fontSize = 11.sp, color = TextMuted)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.7f..1.4f)
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("hi-IN" to "Hindi", "en-US" to "English", "pa-IN" to "Punjabi", "ta-IN" to "Tamil", "es-ES" to "Spanish").forEach { (tag, label) ->
                FilterChip(
                    selected = langTag == tag,
                    onClick = { langTag = tag },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        TextButton(onClick = { showCloneApi = !showCloneApi }) {
            Text(
                if (showCloneApi) "Hide ElevenLabs True Voice Clone API ▲" else "Optional: Use Own ElevenLabs Voice Clone API ▼",
                fontSize = 11.sp,
                color = ElectricCyan
            )
        }
        OutlinedButton(onClick = onPickVoiceSample, modifier = Modifier.fillMaxWidth()) {
            Text(voiceSampleLabel?.let { "Sample: $it (Change)" } ?: "Upload / Pick Voice Sample (WAV/MP3)")
        }
        if (showCloneApi) {
            OutlinedTextField(
                value = elevenKey,
                onValueChange = { elevenKey = it },
                label = { Text("ElevenLabs API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = elevenVoiceId,
                onValueChange = { elevenVoiceId = it },
                label = { Text("Existing Cloned Voice ID (Optional for TTS)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (hasVoiceSample && elevenKey.isNotBlank()) {
                Button(
                    onClick = { onCloneFromSample(script, "KB_User_Voice", elevenKey) },
                    enabled = script.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clone Sample & Generate (ElevenLabs API)")
                }
            }
        }

        Button(
            onClick = { onSynthesizeVoice(script, langTag, pitch, speed, elevenKey, elevenVoiceId) },
            enabled = script.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate AI Voiceover to Track A1")
        }
    }
}


@Composable
fun ClipTransformVolumeSheet(
    clip: VideoClip,
    onUpdateClip: (VideoClip) -> Unit,
    onRotate90: () -> Unit,
    onReplaceClip: () -> Unit,
    onResetTransform: () -> Unit = {},
    onGenerateProxy: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Clip Volume, Rotate & Fade (${clip.title})", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Mute Clip Audio", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = clip.muted,
                onCheckedChange = { onUpdateClip(clip.copy(muted = it)) }
            )
        }

        Text("Clip Volume: ${(clip.volume * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
        Slider(
            value = clip.volume,
            onValueChange = { onUpdateClip(clip.copy(volume = it)) },
            valueRange = 0f..1.5f
        )

        Text("Fade In: ${clip.fadeInMs}ms", fontSize = 11.sp, color = TextMuted)
        Slider(
            value = clip.fadeInMs.toFloat(),
            onValueChange = { onUpdateClip(clip.copy(fadeInMs = it.toLong())) },
            valueRange = 0f..2000f
        )

        Text("Fade Out: ${clip.fadeOutMs}ms", fontSize = 11.sp, color = TextMuted)
        Slider(
            value = clip.fadeOutMs.toFloat(),
            onValueChange = { onUpdateClip(clip.copy(fadeOutMs = it.toLong())) },
            valueRange = 0f..2000f
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onRotate90, modifier = Modifier.weight(1f)) {
                Text("Rotate 90° (${clip.rotationDeg.toInt()}°)")
            }
            Button(onClick = onReplaceClip, modifier = Modifier.weight(1f)) {
                Text("Replace Clip")
            }
        }
        OutlinedButton(onClick = onResetTransform, modifier = Modifier.fillMaxWidth()) {
            Text("Reset Zoom & Pan (${"%.1f".format(clip.zoomScale)}x)")
        }
        OutlinedButton(onClick = onGenerateProxy, modifier = Modifier.fillMaxWidth()) {
            Text(if (clip.proxyUri.isNotBlank()) "540p Smooth Proxy: Active ✓" else "Generate 540p Smooth Proxy (Low-RAM)")
        }
    }
}


@Composable
fun WatermarkRemoverSheet(
    spec: WatermarkRemoverSpec,
    onUpdate: (WatermarkRemoverSpec) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Manual / Custom Object & Watermark Remover", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Watermark Removal", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = spec.enabled, onCheckedChange = { onUpdate(spec.copy(enabled = it)) })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !spec.cropMode,
                onClick = { onUpdate(spec.copy(enabled = true, cropMode = false)) },
                label = { Text("Smart Patch Blur") }
            )
            FilterChip(
                selected = spec.cropMode,
                onClick = { onUpdate(spec.copy(enabled = true, cropMode = true)) },
                label = { Text("Crop Out Edge (14%)") }
            )
        }

        if (!spec.cropMode) {
            Text("Quick Corner Presets", style = MaterialTheme.typography.labelSmall, color = com.aistudio.kbeditor.r7v4qx.ui.theme.WarmAmber)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Top-Right" to (0.84f to 0.12f),
                    "Top-Left" to (0.16f to 0.12f),
                    "Bottom-Right" to (0.84f to 0.88f),
                    "Bottom-Left" to (0.16f to 0.88f)
                ).forEach { (label, pos) ->
                    AssistChip(
                        onClick = { onUpdate(spec.copy(enabled = true, posX = pos.first, posY = pos.second)) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
            Text("Box X: ${(spec.posX * 100).toInt()}% • Y: ${(spec.posY * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
            Slider(value = spec.posX, onValueChange = { onUpdate(spec.copy(posX = it)) }, valueRange = 0.1f..0.9f)
            Slider(value = spec.posY, onValueChange = { onUpdate(spec.copy(posY = it)) }, valueRange = 0.08f..0.92f)
            Text("Box Width: ${(spec.boxWidth * 100).toInt()}% • Height: ${(spec.boxHeight * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
            Slider(value = spec.boxWidth, onValueChange = { onUpdate(spec.copy(boxWidth = it)) }, valueRange = 0.08f..0.5f)
            Slider(value = spec.boxHeight, onValueChange = { onUpdate(spec.copy(boxHeight = it)) }, valueRange = 0.04f..0.25f)
        }
    }
}
