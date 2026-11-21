package com.aistudio.kbeditor.r7v4qx.ui

import com.aistudio.kbeditor.r7v4qx.model.MaskType

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.aistudio.kbeditor.r7v4qx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun KbEditorScreen(vm: KbEditorViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val config = LocalConfiguration.current
    val isExpandedLayout = config.screenWidthDp >= 720
    val (playheadClip, playheadClipStartMs) = remember(state.project.videoClips, state.playheadMs) {
        var acc = 0L
        var found = state.project.videoClips.firstOrNull()
        var foundStart = 0L
        for (c in state.project.videoClips) {
            val end = acc + c.activeDurationMs
            if (state.playheadMs in acc..end) {
                found = c
                foundStart = acc
                break
            }
            acc = end
        }
        found to foundStart
    }
    val selectedClip = state.project.videoClips.getOrNull(state.selectedVideoIndex)
    val previewClip = if (state.isPlaying) playheadClip else (playheadClip ?: selectedClip)

    var isInEditor by remember { mutableStateOf(false) }
    var pendingTemplateId by remember { mutableStateOf<String?>(null) }
    val voiceSamplePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.setVoiceSampleUri(uri)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            vm.importMultipleClips(uris)
            pendingTemplateId?.let { tid ->
                vm.applyHomeTemplate(tid)
                pendingTemplateId = null
                isInEditor = true
            }
        } else {
            pendingTemplateId = null
        }
    }
    val pipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importPipOverlay(uri)
    }
    val facePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.setFaceSwapSourcePhoto(uri)
    }
    val enhancePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.enhancePhotoUri(uri)
    }
    val replaceClipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.replaceSelectedClip(uri)
    }
    var pendingReplacePipId by remember { mutableStateOf<String?>(null) }
    val replacePipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pid = pendingReplacePipId
        if (uri != null && pid != null) vm.replacePipClip(pid, uri)
        pendingReplacePipId = null
    }
    val ctx = LocalContext.current
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.toggleMicRecording()
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importLocalAudioUri(uri)
    }
    var copilotInput by remember { mutableStateOf("") }
    var isProAiMode by remember { mutableStateOf(false) }
    var isFullScreenPreview by remember { mutableStateOf(false) }

    val multiPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            isInEditor = true
            isProAiMode = true
            vm.createPhotoToVideoReel(uris)
        }
    }

    if (!isInEditor) {
        Box(modifier = Modifier.fillMaxSize()) {
            StudioHomeScreen(
                project = state.project,
                account = state.account,
                onNewVideo = {
                    isInEditor = true
                    videoPicker.launch(arrayOf("video/*", "image/*"))
                },
                onOpenEditorWithSheet = { sheet ->
                    isInEditor = true
                    isProAiMode = sheet == ActiveStudioSheet.AUTO_CREATOR || sheet == ActiveStudioSheet.AUDIO_DUBBING
                    vm.openSheet(sheet)
                },
                onOpenEditor = { isInEditor = true },
                onOpenAccount = { vm.openLoginDialog(true) },
                onApplyTemplate = { tid ->
                    if (state.project.videoClips.any { it.mediaUri.isNotBlank() }) {
                        vm.applyHomeTemplate(tid)
                        isInEditor = true
                    } else {
                        pendingTemplateId = tid
                        videoPicker.launch(arrayOf("video/*", "image/*"))
                    }
                },
                onPhotoToVideo = { multiPhotoPicker.launch(arrayOf("image/*")) },
                savedProjects = state.savedProjects,
                onNewDraft = {
                    vm.createNewDraftProject()
                    isInEditor = true
                },
                onOpenProject = { pid -> vm.openSavedProject(pid) },
                onDeleteProject = vm::deleteSavedProject
            )
            if (state.showLoginDialog) {
                ProAccountLoginDialog(
                    accountState = state.account,
                    onLogin = vm::loginProAccount,
                    onLogout = vm::logoutProAccount,
                    onDismiss = { vm.openLoginDialog(false) }
                )
            }
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            StudioTopBar(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                isBusy = state.isBusy,
                isLoggedIn = state.account.isLoggedIn,
                dailyCoins = state.account.dailyCoins,
                resolutionLabel = state.project.exportSettings.resolution.label,
                onOpenAccount = { vm.openLoginDialog(true) },
                onImport = {
                    videoPicker.launch(arrayOf("video/*", "image/*"))
                },
                onUndo = vm::undo,
                onRedo = vm::redo,
                onReset = { isInEditor = false },
                onExport = { vm.openSheet(ActiveStudioSheet.EXPORT_PRO) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.38f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isMaskEditMode) {
                    Surface(
                        color = Color(0xFF0E2936),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Drag, pinch & rotate Mask on screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = vm::stopMaskScreenEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("Done", style = MaterialTheme.typography.labelSmall, color = WarmAmber)
                            }
                        }
                    }
                }
                if (state.isMagicEraserMode) {
                    Surface(
                        color = Color(0xFF0E2936),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Magic Brush: Paint object on preview (${previewClip?.magicEraserPoints?.size ?: 0}/24)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = vm::clearMagicEraser, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall, color = WarmAmber)
                            }
                            TextButton(onClick = vm::toggleMagicEraserMode, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text("Done", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
                if (state.isMotionTrackerPicking) {
                    Surface(
                        color = Color(0xFF0E2936),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tap subject on video/photo to lock ${state.pendingTrackMode} track",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = vm::cancelTapMotionTracking, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("Cancel", style = MaterialTheme.typography.labelSmall, color = WarmAmber)
                            }
                        }
                    }
                }
                LaunchedEffect(state.statusBanner, state.isBusy) {
                    if (state.statusBanner != null && !state.isBusy) {
                        kotlinx.coroutines.delay(2600L)
                        vm.clearStatusBanner()
                    }
                }
                AnimatedVisibility(
                    visible = state.statusBanner != null || state.isBusy,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = SlateElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.statusBanner ?: "Processing...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = vm::clearStatusBanner, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted)
                                }
                            }
                            if (state.isBusy) {
                                LinearProgressIndicator(
                                    color = ElectricCyan,
                                    trackColor = SlateBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                )
                            }
                        }
                    }
                }

                VerticalPreviewStage(
                    project = state.project,
                    playheadMs = state.playheadMs,
                    isPlaying = state.isPlaying,
                    selectedClip = previewClip,
                    clipLocalMs = (state.playheadMs - playheadClipStartMs).coerceAtLeast(0L),
                    isMotionTrackerMode = state.activeSheet == ActiveStudioSheet.MOTION_TRACKER || state.isMotionTrackerPicking,
                    isMaskEditMode = (state.activeSheet == ActiveStudioSheet.VFX_CHROMA_MASK || state.isMaskEditMode) && (previewClip?.maskType != null && previewClip.maskType != MaskType.NONE),
                    isMagicEraserMode = state.isMagicEraserMode,
                    onPaintEraser = vm::addMagicEraserPoint,
                    onTrackerTap = { x, y -> vm.runMotionTracking(x, y, state.pendingTrackMode) },
                    onTransformMask = vm::transformSelectedMask,
                    onDragCaption = vm::moveCaptionPosition,
                    onDragPip = vm::movePipPosition,
                    onDragWatermark = vm::moveWatermarkBox,
                    onTransformVideo = vm::transformSelectedClip,
                    onTransformPip = vm::transformPipOverlay,
                    onTransformCaption = vm::transformCaption,
                    abBypassNoiseReduction = state.abBypassNoiseReduction,
                    selectedPipId = state.selectedPipId,
                    onSelectPip = vm::selectPip,
                    onDeletePip = vm::deletePipClip,
                    onResetTransform = vm::resetClipTransform,
                    isFullScreen = isFullScreenPreview,
                    onToggleFullScreen = { isFullScreenPreview = !isFullScreenPreview },
                    modifier = Modifier.weight(1f)
                )

                TransportControlsBar(
                    playheadMs = state.playheadMs,
                    totalDurationMs = state.project.totalDurationMs,
                    isPlaying = state.isPlaying,
                    hasKeyframes = selectedClip?.keyframes?.isNotEmpty() == true,
                    aspectLabel = state.project.aspectRatio.label,
                    safeZoneVisible = state.project.safeZoneVisible,
                    onTogglePlay = vm::togglePlayPause,
                    onToggleKeyframe = vm::toggleKeyframeAtPlayhead,
                    onOpenKeyframeGraph = { vm.openSheet(ActiveStudioSheet.KEYFRAME_GRAPH) },
                    onOpenRatio = { vm.openSheet(ActiveStudioSheet.CANVAS_REFRAME) },
                    onToggleSafeZone = vm::toggleSafeZone,
                    onImport = { videoPicker.launch(arrayOf("video/*", "image/*")) }
                )

                if (!isFullScreenPreview) {
                    MultiTrackTimeline(
                    project = state.project,
                    playheadMs = state.playheadMs,
                    selectedVideoIndex = state.selectedVideoIndex,
                    onSelectVideo = vm::selectVideoClip,
                    selectedPipId = state.selectedPipId,
                    onSelectPip = vm::selectPip,
                    onMovePipTiming = vm::movePipTiming,
                    onMoveCaptionTiming = vm::moveCaptionTiming,
                    onTrimClipStart = vm::trimClipStart,
                    onTrimClipEnd = vm::trimClipEnd,
                    onSeekMs = vm::seekTo,
                    onToggleTrack = vm::updateTrackState,
                    onOpenSheet = vm::openSheet,
                    onSplitClip = vm::splitSelectedClipAtPlayhead,
                    onRippleDelete = vm::rippleDeleteSelectedClip,
                    onMoveClip = vm::moveSelectedClip,
                    onAddClip = { videoPicker.launch(arrayOf("video/*", "image/*")) },
                    onDuplicateClip = vm::duplicateSelectedClip,
                    onFlipClip = vm::flipSelectedClip,
                    onFreezeFrame = vm::freezeFrameAtPlayhead,
                    isProAiMode = isProAiMode,
                    onToggleProAiMode = { isProAiMode = it },
                    onPhotoToVideo = { multiPhotoPicker.launch(arrayOf("image/*")) },
                    onRotateClip = vm::rotateSelectedClip90,
                    onReplaceClip = { replaceClipPicker.launch(arrayOf("video/*", "image/*")) },
                    isMagicEraserMode = state.isMagicEraserMode,
                    onToggleMagicEraser = vm::toggleMagicEraserMode,
                    onClearMagicEraser = vm::clearMagicEraser
                    )
                }

                if (!isExpandedLayout && (isProAiMode || state.aiDrawerExpanded)) {
                    // Compact Handheld Docked AI Copilot Command Bar (shown only in Pro AI Edit mode or when toggled)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateBase, RoundedCornerShape(12.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = { vm.toggleAiDrawer() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Drawer", tint = ElectricCyan)
                        }
                        OutlinedTextField(
                            value = copilotInput,
                            onValueChange = { copilotInput = it },
                            placeholder = { Text("AI Copilot: e.g. Teal Orange + Flash In", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (copilotInput.isNotBlank()) {
                                    vm.runAiCopilot(copilotInput)
                                    copilotInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Run AI", tint = ElectricCyan)
                        }
                    }
                }
            }

            if (isExpandedLayout) {
                AiSidePanel(
                    statusBanner = state.statusBanner,
                    onRunCopilot = vm::runAiCopilot,
                    onOpenAutoCreator = { vm.openSheet(ActiveStudioSheet.AUTO_CREATOR) },
                    modifier = Modifier.width(310.dp)
                )
            }
        }

        if (state.showLoginDialog) {
            ProAccountLoginDialog(
                accountState = state.account,
                onLogin = vm::loginProAccount,
                onLogout = vm::logoutProAccount,
                onDismiss = { vm.openLoginDialog(false) }
            )
        }

        if (state.isExporting) {
            Surface(
                color = Color(0xEE111827),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (state.exportProgress ?: 0f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f)
                    )
                    Text("${((state.exportProgress ?: 0f) * 100).toInt()}%", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = vm::cancelExport, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Cancel", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.activeSheet != ActiveStudioSheet.NONE) {
            Surface(
                color = SlateBase,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 285.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SlateBorder)
                        )
                        TextButton(
                            onClick = { vm.openSheet(ActiveStudioSheet.NONE) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Done", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                    when (state.activeSheet) {
                        ActiveStudioSheet.COLOR_WHEELS -> previewClip?.let { clip ->
                            ColorWheelsPanel(
                                grade = clip.colorGrade,
                                lastCorrectionReport = state.lastCorrectionReport,
                                onUpdateGrade = { g -> vm.updateSelectedClip { it.copy(colorGrade = g) } },
                                onAiColorGrade = vm::runAiColorGrade,
                                onAutoColorCorrect = vm::runAutoColorCorrection
                            )
                        }
                        ActiveStudioSheet.SPEED_RAMP -> previewClip?.let { clip ->
                            SpeedRampSheet(
                                clip = clip,
                                onUpdateClip = { updated -> vm.updateSelectedClip { updated } }
                            )
                        }
                        ActiveStudioSheet.VFX_CHROMA_MASK -> previewClip?.let { clip ->
                            VfxChromaMaskSheet(
                                clip = clip,
                                onUpdateClip = { updated -> vm.updateSelectedClip { updated } },
                                onEditMaskOnScreen = vm::startMaskScreenEdit
                            )
                        }
                        ActiveStudioSheet.MOTION_TRACKER -> MotionTrackerPanel(
                            pointCount = previewClip?.motionTrackPoints?.size ?: 0,
                            trackMode = previewClip?.motionTrackMode ?: state.pendingTrackMode,
                            onModeChange = vm::setMotionTrackMode,
                            onStartTapOnVideo = vm::startTapMotionTracking,
                            onRunTrack = vm::runMotionTracking,
                            onClearTrack = vm::clearMotionTracking
                        )
                        ActiveStudioSheet.PIP_OVERLAY -> PipOverlaySheet(
                            pipClips = state.project.pipClips,
                            onPickPipMedia = {
                                pipPicker.launch(arrayOf("image/*", "video/*"))
                            },
                            onAddOpticalPreset = { preset, blend ->
                                vm.importPipOverlay(uri = null, opticalPreset = preset, blendMode = blend)
                            },
                            onMoveSelectedToOverlay = { blend ->
                                vm.moveSelectedClipToOverlay(blend)
                                vm.openSheet(ActiveStudioSheet.NONE)
                            },
                            onDuplicatePip = vm::duplicatePipClip,
                            onReplacePip = { pid ->
                                pendingReplacePipId = pid
                                replacePipPicker.launch(arrayOf("image/*", "video/*"))
                            },
                            onDeletePip = vm::deletePipClip
                        )
                        ActiveStudioSheet.CAPTION_3D -> Caption3DSheet(
                            captions = state.project.captionClips,
                            dailyCoins = state.account.dailyCoins,
                            onAddCaption = { txt, anim, style, cHex, aHex, fSp, pY, fStyle ->
                                vm.addOrUpdateCaption(txt, anim, previewClip?.id, style, cHex, aHex, fSp, pY, fStyle)
                                vm.openSheet(ActiveStudioSheet.NONE)
                            },
                            onRunAutoCaptions = { lang, style, cHex, aHex, fSp, pY, customLang ->
                                vm.runAutoCaptions(lang, style, cHex, aHex, fSp, pY, customLang)
                            },
                            onApplyDesignToAll = { style, anim, cHex, aHex, fSp, pY, fStyle ->
                                vm.applyCaptionDesignToAll(style, anim, cHex, aHex, fSp, pY, fStyle)
                            },
                            onDeleteCaption = vm::deleteCaption,
                            onClearAll = vm::clearAllCaptions
                        )
                        ActiveStudioSheet.AUDIO_DUBBING -> AudioDubbingSheet(
                            dailyCoins = state.account.dailyCoins,
                            onRunAutoDub = vm::runAutoDubbing
                        )
                        ActiveStudioSheet.AUDIO_PRO -> AudioProSheet(
                            audioClip = state.project.audioClips.firstOrNull(),
                            audioClips = state.project.audioClips,
                            onExtractAudio = vm::extractAudioFromSelectedClip,
                            onUpdateDsp = vm::updateAudioDsp,
                            onUpdateEchoRemoval = vm::updateEchoRemoval,
                            onDetectBeats = vm::detectBeatsOnAudio,
                            onGenerateVoiceover = vm::generateVoiceover,
                            onAddSfx = vm::addCapCutSfx,
                            onAddSfxVariant = vm::addCapCutSfxVariant,
                            onDeleteAudioClip = vm::deleteAudioClip,
                            onImportOnlineAudio = vm::importOnlineAudioUrl,
                            onPickLocalAudio = { audioPicker.launch(arrayOf("audio/*")) },
                            isRecordingMic = state.isRecordingMic,
                            onAutoBeatCut = {
                                vm.runAutoBeatCutAi()
                                vm.openSheet(ActiveStudioSheet.NONE)
                            },
                            onToggleMicRecord = {
                                val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm || state.isRecordingMic) {
                                    vm.toggleMicRecording()
                                } else {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            denoiseBackendLabel = state.denoiseBackendLabel,
                            abBypassNoiseReduction = state.abBypassNoiseReduction,
                            onToggleAbBypass = vm::toggleAbNoiseBypass,
                            onUpdateNoiseStrength = vm::updateNoiseReductionStrength,
                            onRunOfflineDenoise = vm::runOfflineNoiseReduction
                        )
                        ActiveStudioSheet.AUTO_CREATOR -> AutoCreatorSheet(
                            dailyCoins = state.account.dailyCoins,
                            lastCorrectionReport = state.lastCorrectionReport,
                            onRunAutoCreator = vm::runAiAutoCreator,
                            onGenerateAiBroll = vm::generateAiBroll
                        )
                        ActiveStudioSheet.EXPORT_PRO -> ExportProSheet(
                            settings = state.project.exportSettings,
                            exportProgress = state.exportProgress,
                            lastExportPath = state.lastExportPath,
                            viralKit = state.viralExportKit,
                            onUpdateSettings = vm::updateExportSettings,
                            onStartExport = vm::startExport,
                            onGenerateViralKit = vm::generateViralExportKit,
                            onCopyViralText = vm::copyViralKitText
                        )
                        ActiveStudioSheet.CANVAS_REFRAME -> CanvasReframeSheet(
                            project = state.project,
                            onUpdateCanvas = vm::updateCanvasSettings,
                            onSmartAutoReframe = vm::runSmartAutoReframe
                        )
                        ActiveStudioSheet.KEYFRAME_GRAPH -> KeyframeGraphSheet(
                            keyframes = previewClip?.keyframes.orEmpty(),
                            currentNormT = vm.currentClipNormalizedTime(),
                            onToggleKeyframe = vm::toggleKeyframeAtPlayhead,
                            onUpdateKeyframe = vm::updateKeyframeAtPlayhead,
                            onClearAll = vm::clearAllKeyframes,
                            onApplyPreset = vm::applyKeyframePreset,
                            onSelectCurve = vm::updateKeyframeCurve
                        )
                        ActiveStudioSheet.TRANSITION_FX -> previewClip?.let { clip ->
                            TransitionsAndEffectsSheet(
                                clip = clip,
                                onUpdateClip = { updated -> vm.updateSelectedClip { updated } }
                            )
                        }
                        ActiveStudioSheet.FACE_SWAP -> previewClip?.let { clip ->
                            FaceSwapSheet(
                                faceSwap = clip.faceSwap,
                                onPickFacePhoto = { facePicker.launch(arrayOf("image/*")) },
                                onUpdateFaceSwap = { fs -> vm.updateSelectedClip { it.copy(faceSwap = fs) } }
                            )
                        }
                        ActiveStudioSheet.STICKERS -> StickersSheet(
                            pipClips = state.project.pipClips,
                            onAddSticker = vm::addStickerOverlay,
                            onDeleteSticker = vm::deletePipClip
                        )
                        ActiveStudioSheet.ENHANCER -> EnhancerSheet(
                            audioClip = state.project.audioClips.firstOrNull(),
                            onPickAndEnhancePhoto = { enhancePhotoPicker.launch(arrayOf("image/*")) },
                            onEnhanceCurrentPhoto = { vm.enhancePhotoUri(null) },
                            onEnhanceVideo = vm::enhanceVideoPreset,
                            onEnhanceVoice = vm::enhanceVoiceOneTap
                        )
                        ActiveStudioSheet.VOICE_CLONE -> VoiceCloneSheet(
                            generatedScript = state.generatedVoiceScript,
                            isRecordingSample = state.isRecordingVoiceSample,
                            matchedPitch = state.matchedPitch,
                            matchedSpeed = state.matchedSpeed,
                            matchedLangTag = state.matchedLangTag,
                            voiceProfileSummary = state.voiceProfileSummary,
                            voiceSampleLabel = state.voiceSampleLabel,
                            hasVoiceSample = !state.voiceSampleUri.isNullOrBlank(),
                            onToggleVoiceMatcher = {
                                val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm || state.isRecordingVoiceSample) {
                                    vm.toggleVoiceSampleMatcher()
                                } else {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onPickVoiceSample = { voiceSamplePicker.launch(arrayOf("audio/*")) },
                            onGenerateScript = vm::generateAiVoiceScript,
                            onSynthesizeVoice = vm::synthesizeProVoiceover,
                            onCloneFromSample = vm::cloneVoiceFromSampleAndSynthesize
                        )
                        ActiveStudioSheet.CLIP_EDIT -> previewClip?.let { clip ->
                            ClipTransformVolumeSheet(
                                clip = clip,
                                onUpdateClip = { updated -> vm.updateSelectedClip { updated } },
                                onRotate90 = vm::rotateSelectedClip90,
                                onReplaceClip = { replaceClipPicker.launch(arrayOf("video/*", "image/*")) },
                                onResetTransform = vm::resetClipTransform,
                                onGenerateProxy = vm::generateProxyForSelectedClip
                            )
                        }
                        ActiveStudioSheet.WATERMARK_REMOVER -> previewClip?.let { clip ->
                            WatermarkRemoverSheet(
                                spec = clip.watermarkRemover,
                                onUpdate = { wm -> vm.updateSelectedClip { it.copy(watermarkRemover = wm) } }
                            )
                        }
                        ActiveStudioSheet.NONE -> Unit
                    }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        }
    }
}

@Composable
private fun StudioTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isBusy: Boolean,
    isLoggedIn: Boolean,
    dailyCoins: Int,
    resolutionLabel: String,
    onOpenAccount: () -> Unit,
    onImport: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onReset, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "New Project", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            FilledTonalButton(
                onClick = onOpenAccount,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = if (isLoggedIn) "${dailyCoins}C PRO" else "Login 50C",
                    fontSize = 10.sp,
                    color = WarmAmber
                )
            }
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ElectricCyan)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) Color.White else TextMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) Color.White else TextMuted, modifier = Modifier.size(18.dp))
            }
            OutlinedButton(
                onClick = onExport,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(resolutionLabel, fontSize = 10.sp, color = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = VioletClip, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Export", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TransportControlsBar(
    playheadMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    hasKeyframes: Boolean,
    aspectLabel: String,
    safeZoneVisible: Boolean,
    onTogglePlay: () -> Unit,
    onToggleKeyframe: () -> Unit,
    onOpenKeyframeGraph: () -> Unit,
    onOpenRatio: () -> Unit,
    onToggleSafeZone: () -> Unit,
    onImport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${formatShortTime(playheadMs)} / ${formatShortTime(totalDurationMs)}",
            fontSize = 11.sp,
            color = TextMuted
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val haptics = LocalHapticFeedback.current
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTogglePlay()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleKeyframe()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Add/Remove Keyframe",
                    tint = if (hasKeyframes) WarmAmber else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onOpenKeyframeGraph, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Keyframe Graph",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onOpenRatio,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text(aspectLabel, fontSize = 10.sp, color = ElectricCyan)
            }
            IconButton(onClick = onToggleSafeZone, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = "Safe Zone",
                    tint = if (safeZoneVisible) WarmAmber else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onImport, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = "Import Video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatShortTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSec / 60L, totalSec % 60L)
}