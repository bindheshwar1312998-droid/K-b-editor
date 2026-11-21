package com.aistudio.kbeditor.r7v4qx.ui

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.aistudio.kbeditor.r7v4qx.ai.GeminiStudioEngine
import com.aistudio.kbeditor.r7v4qx.ai.ViralExportKit
import com.aistudio.kbeditor.r7v4qx.audio.AudioBeatDetector
import com.aistudio.kbeditor.r7v4qx.audio.AudioExtractorEngine
import com.aistudio.kbeditor.r7v4qx.audio.CapCutSfxPreset
import com.aistudio.kbeditor.r7v4qx.audio.MicRecorderEngine
import com.aistudio.kbeditor.r7v4qx.audio.StudioSfxEngine
import com.aistudio.kbeditor.r7v4qx.audio.AiAudioDenoiseEngine
import com.aistudio.kbeditor.r7v4qx.audio.VoiceoverTtsEngine
import com.aistudio.kbeditor.r7v4qx.media.AntiBugCrashGuard
import com.aistudio.kbeditor.r7v4qx.media.ProxyMediaEngine
import com.aistudio.kbeditor.r7v4qx.media.VideoExportEngine
import com.aistudio.kbeditor.r7v4qx.model.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.aistudio.kbeditor.r7v4qx.storage.AccountCoinManager
import com.aistudio.kbeditor.r7v4qx.storage.ProAccountState
import com.aistudio.kbeditor.r7v4qx.storage.ProjectStorage
import com.aistudio.kbeditor.r7v4qx.vfx.CurveEvaluators
import com.aistudio.kbeditor.r7v4qx.vision.PhotoEnhancerEngine
import com.aistudio.kbeditor.r7v4qx.vision.SmartVisionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveStudioSheet {
    NONE,
    COLOR_WHEELS,
    SPEED_RAMP,
    VFX_CHROMA_MASK,
    MOTION_TRACKER,
    PIP_OVERLAY,
    CAPTION_3D,
    AUDIO_PRO,
    AUDIO_DUBBING,
    AUTO_CREATOR,
    EXPORT_PRO,
    CANVAS_REFRAME,
    KEYFRAME_GRAPH,
    TRANSITION_FX,
    FACE_SWAP,
    STICKERS,
    ENHANCER,
    VOICE_CLONE,
    CLIP_EDIT,
    WATERMARK_REMOVER
}

data class StudioUiState(
    val project: KbEditorProject = KbEditorProject(),
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedVideoIndex: Int = 0,
    val selectedPipId: String? = null,
    val selectedCaptionId: String? = null,
    val selectedAudioId: String? = null,
    val activeSheet: ActiveStudioSheet = ActiveStudioSheet.NONE,
    val isMotionTrackerPicking: Boolean = false,
    val pendingTrackMode: String = "2D",
    val isMaskEditMode: Boolean = false,
    val isMagicEraserMode: Boolean = false,
    val aiDrawerExpanded: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isBusy: Boolean = false,
    val statusBanner: String? = null,
    val lastCorrectionReport: String? = null,
    val generatedVoiceScript: String = "",
    val isRecordingMic: Boolean = false,
    val isRecordingVoiceSample: Boolean = false,
    val matchedPitch: Float? = null,
    val matchedSpeed: Float? = null,
    val matchedLangTag: String? = null,
    val voiceProfileSummary: String? = null,
    val voiceSampleUri: String? = null,
    val voiceSampleLabel: String? = null,
    val abBypassNoiseReduction: Boolean = false,
    val denoiseBackendLabel: String = "DSP Fallback (No AI model bundled)",
    val isExporting: Boolean = false,
    val savedProjects: List<KbEditorProject> = emptyList(),
    val viralExportKit: ViralExportKit? = null,
    val exportProgress: Float? = null,
    val lastExportPath: String? = null,
    val account: ProAccountState = ProAccountState(),
    val showLoginDialog: Boolean = false
)

@UnstableApi
class KbEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = ProjectStorage(application)
    private val accountManager = AccountCoinManager(application)
    private val visionEngine = SmartVisionEngine(application)
    private val photoEnhancer = PhotoEnhancerEngine(application)
    private val audioExtractor = AudioExtractorEngine(application)
    private val beatDetector = AudioBeatDetector(application)
    private val ttsEngine = VoiceoverTtsEngine(application)
    private val sfxEngine = StudioSfxEngine(application)
    private val micRecorder = MicRecorderEngine(application)
    private val exportEngine = VideoExportEngine(application)
    private val geminiEngine = GeminiStudioEngine(application)
    private val proxyEngine = ProxyMediaEngine(application)
    private val denoiseEngine = AiAudioDenoiseEngine(application)

    private val _uiState = MutableStateFlow(
        accountManager.loadState().let { acc ->
            StudioUiState(
                project = storage.loadProject(),
                savedProjects = storage.loadAllProjects(),
                account = acc,
                showLoginDialog = false
            )
        }
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var activeExportJob: kotlinx.coroutines.Job? = null

    private fun updateProject(recordUndo: Boolean = true, transform: (KbEditorProject) -> KbEditorProject) {
        _uiState.update { state ->
            if (recordUndo) {
                storage.pushUndoSnapshot(state.project)
            }
            val next = transform(state.project)
            storage.saveProject(next)
            state.copy(
                project = next,
                savedProjects = storage.loadAllProjects(),
                canUndo = storage.canUndo(),
                canRedo = storage.canRedo()
            )
        }
    }

    fun undo() {
        val prev = storage.undo(_uiState.value.project) ?: return
        _uiState.update {
            it.copy(
                project = prev,
                canUndo = storage.canUndo(),
                canRedo = storage.canRedo(),
                statusBanner = "Undid last edit"
            )
        }
    }

    fun redo() {
        val next = storage.redo(_uiState.value.project) ?: return
        _uiState.update {
            it.copy(
                project = next,
                canUndo = storage.canUndo(),
                canRedo = storage.canRedo(),
                statusBanner = "Redid edit"
            )
        }
    }

    fun createNewDraftProject() {
        storage.clearHistory()
        val count = (_uiState.value.savedProjects.size + 1)
        val fresh = KbEditorProject(projectName = "KB Draft #$count")
        storage.saveProject(fresh)
        _uiState.update {
            it.copy(
                project = fresh,
                savedProjects = storage.loadAllProjects(),
                selectedVideoIndex = 0,
                playheadMs = 0L,
                viralExportKit = null,
                statusBanner = "Created ${fresh.projectName}"
            )
        }
    }

    fun openSavedProject(projectId: String) {
        val target = _uiState.value.savedProjects.firstOrNull { it.projectId == projectId } ?: return
        storage.clearHistory()
        storage.saveProject(target)
        _uiState.update {
            it.copy(
                project = target,
                selectedVideoIndex = 0,
                playheadMs = 0L,
                viralExportKit = null,
                statusBanner = "Opened '${target.projectName}'"
            )
        }
    }

    fun deleteSavedProject(projectId: String) {
        val nextList = storage.deleteProjectFromLibrary(projectId)
        val active = if (_uiState.value.project.projectId == projectId) nextList.first() else _uiState.value.project
        storage.saveProject(active)
        _uiState.update {
            it.copy(
                project = active,
                savedProjects = nextList,
                statusBanner = "Deleted draft project"
            )
        }
    }

    fun generateViralExportKit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Generating AI Title, Hashtags & HD Thumbnail...") }
            val res = geminiEngine.generateViralExportKit(_uiState.value.project)
            res.onSuccess { kit ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        viralExportKit = kit,
                        statusBanner = "Viral Title, Hashtags & Thumbnail Ready!"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "Viral kit failed") }
            }
        }
    }

    fun copyViralKitText(text: String) {
        val cb = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("KB Viral Kit", text))
        _uiState.update { it.copy(statusBanner = "Copied Title & Hashtags!") }
    }

    fun resetProject() {
        storage.clearHistory()
        val fresh = KbEditorProject()
        storage.saveProject(fresh)
        _uiState.update {
            StudioUiState(
                project = fresh,
                statusBanner = "Reset studio sequence"
            )
        }
    }

    fun togglePlayPause() {
        val currentlyPlaying = _uiState.value.isPlaying
        if (currentlyPlaying) {
            playbackJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            _uiState.update { s ->
                val total = s.project.totalDurationMs.coerceAtLeast(300L)
                val restartFromStart = s.playheadMs >= (total - 40L).coerceAtLeast(0L)
                s.copy(
                    isPlaying = true,
                    playheadMs = if (restartFromStart) 0L else s.playheadMs,
                    selectedVideoIndex = if (restartFromStart) 0 else s.selectedVideoIndex
                )
            }
            playbackJob?.cancel()
            playbackJob = viewModelScope.launch {
                var lastTickMs = SystemClock.elapsedRealtime()
                while (true) {
                    delay(16L)
                    val nowMs = SystemClock.elapsedRealtime()
                    val dtMs = (nowMs - lastTickMs).coerceIn(1L, 80L)
                    lastTickMs = nowMs
                    var reachedEnd = false
                    _uiState.update { s ->
                        if (!s.isPlaying) {
                            reachedEnd = true
                            return@update s
                        }
                        val total = s.project.totalDurationMs.coerceAtLeast(300L)
                        val nextMs = s.playheadMs + dtMs
                        if (nextMs >= total) {
                            reachedEnd = true
                            return@update s.copy(
                                playheadMs = total,
                                selectedVideoIndex = s.project.videoClips.lastIndex.coerceAtLeast(0),
                                isPlaying = false
                            )
                        }
                        var acc = 0L
                        var phIdx = s.selectedVideoIndex
                        for (i in s.project.videoClips.indices) {
                            val end = acc + s.project.videoClips[i].activeDurationMs
                            if (nextMs in acc..end) {
                                phIdx = i
                                break
                            }
                            acc = end
                        }
                        s.copy(playheadMs = nextMs, selectedVideoIndex = phIdx)
                    }
                    if (reachedEnd) break
                }
            }
        }
    }

    fun seekTo(ms: Long) {
        val clamped = ms.coerceIn(0L, _uiState.value.project.totalDurationMs)
        val clips = _uiState.value.project.videoClips
        var acc = 0L
        var phIdx = _uiState.value.selectedVideoIndex
        for (i in clips.indices) {
            val end = acc + clips[i].activeDurationMs
            if (clamped in acc..end) {
                phIdx = i
                break
            }
            acc = end
        }
        _uiState.update {
            it.copy(playheadMs = clamped, selectedVideoIndex = phIdx)
        }
    }

    fun stepFrames(frameDelta: Int) {
        val frameMs = (1000L / _uiState.value.project.exportSettings.fps.value.coerceAtLeast(24))
        seekTo(_uiState.value.playheadMs + frameDelta * frameMs)
    }

    fun stepSeconds(secDelta: Int) {
        seekTo(_uiState.value.playheadMs + secDelta * 1000L)
    }

    fun selectVideoClip(index: Int) {
        _uiState.update { state ->
            val clips = state.project.videoClips
            val safeIdx = index.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
            val startMs = clips.take(safeIdx).sumOf { it.activeDurationMs }
            state.copy(selectedVideoIndex = safeIdx, playheadMs = startMs)
        }
    }

    fun openSheet(sheet: ActiveStudioSheet) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun toggleAiDrawer(expanded: Boolean? = null) {
        _uiState.update { it.copy(aiDrawerExpanded = expanded ?: !it.aiDrawerExpanded) }
    }

    fun toggleSafeZone() {
        updateProject(recordUndo = false) { it.copy(safeZoneVisible = !it.safeZoneVisible) }
    }

    fun cycleAspectRatio() {
        val all = AspectRatioPreset.entries
        val current = _uiState.value.project.aspectRatio
        val next = all[(all.indexOf(current) + 1) % all.size]
        updateProject { it.copy(aspectRatio = next) }
    }

    fun updateCanvasSettings(
        ratio: AspectRatioPreset = _uiState.value.project.aspectRatio,
        fillMode: CanvasFillMode = _uiState.value.project.canvasFillMode,
        blurRadius: Float = _uiState.value.project.canvasBlurRadius
    ) {
        updateProject {
            it.copy(
                aspectRatio = ratio,
                canvasFillMode = fillMode,
                canvasBlurRadius = blurRadius
            )
        }
    }

    fun updateTrackState(transform: (TrackState) -> TrackState) {
        updateProject(recordUndo = false) { it.copy(tracks = transform(it.tracks)) }
    }

    fun createPhotoToVideoReel(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        val transitions = listOf(
            TransitionType.CROSS_DISSOLVE,
            TransitionType.ZOOM_THROUGH,
            TransitionType.WHIP_PAN,
            TransitionType.FLASH_CUT
        )
        val photoClips = uris.mapIndexed { idx, uri ->
            runCatching {
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = queryDisplayName(uri) ?: "Photo ${idx + 1}"
            val zoomIn = idx % 2 == 0
            VideoClip(
                title = name,
                mediaUri = uri.toString(),
                isAiGeneratedBroll = true,
                sourceDurationMs = 2500L,
                trimStartMs = 0L,
                trimEndMs = 2500L,
                transitionOut = if (idx < uris.lastIndex) transitions[idx % transitions.size] else TransitionType.NONE,
                transitionDurationMs = 450L,
                videoFx = if (idx % 3 == 0) VideoFxPreset.ZOOM_PULSE else VideoFxPreset.NONE,
                colorGrade = ColorGradeSpec(
                    contrast = 1.14f,
                    saturation = 1.12f,
                    warmth = 0.06f,
                    vignette = 0.14f,
                    lutName = "iPhone Cinema"
                ),
                keyframes = listOf(
                    KeyframePoint(normalizedTime = 0f, scale = if (zoomIn) 1.0f else 1.15f),
                    KeyframePoint(normalizedTime = 1f, scale = if (zoomIn) 1.15f else 1.0f)
                )
            )
        }
        updateProject { proj ->
            proj.copy(
                videoClips = photoClips,
                tracks = proj.tracks.copy(v1Visible = true)
            )
        }
        _uiState.update {
            it.copy(
                selectedVideoIndex = 0,
                playheadMs = 0L,
                statusBanner = "Created Photo-to-Video Reel from ${photoClips.size} photos!"
            )
        }
    }

    fun importMultipleClips(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        val added = uris.mapIndexed { idx, uri ->
            runCatching {
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val mime = app.contentResolver.getType(uri).orEmpty()
            val isImg = mime.startsWith("image/")
            val name = queryDisplayName(uri) ?: "Clip ${idx + 1}"
            val durMs = if (isImg) 3000L else queryMediaDurationMs(uri).coerceAtLeast(3000L)
            VideoClip(
                title = name,
                mediaUri = uri.toString(),
                isAiGeneratedBroll = isImg,
                sourceDurationMs = durMs,
                trimStartMs = 0L,
                trimEndMs = durMs
            )
        }
        updateProject { proj ->
            val base = proj.videoClips.filter { it.mediaUri.isNotBlank() }
            val nextClips = base + added
            proj.copy(videoClips = nextClips, tracks = proj.tracks.copy(v1Visible = true))
        }
        _uiState.update {
            it.copy(
                selectedVideoIndex = (_uiState.value.project.videoClips.size - 1).coerceAtLeast(0),
                statusBanner = "Added ${added.size} clip(s) to Timeline V1"
            )
        }
    }

    fun importVideoClip(uri: Uri, title: String = "Imported Clip") {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val resolvedTitle = runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: title

        val realDurationMs = runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(app, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            dur
        }.getOrNull()?.coerceAtLeast(1000L) ?: 8000L

        var newIndex = 0
        updateProject { proj ->
            val clip = VideoClip(
                title = resolvedTitle,
                mediaUri = uri.toString(),
                sourceDurationMs = realDurationMs,
                trimStartMs = 0L,
                trimEndMs = realDurationMs
            )
            val baseList = if (proj.videoClips.all { it.mediaUri.isBlank() }) emptyList() else proj.videoClips
            newIndex = baseList.size
            proj.copy(videoClips = baseList + clip)
        }
        _uiState.update {
            it.copy(
                selectedVideoIndex = newIndex,
                playheadMs = 0L,
                statusBanner = "Loaded '$resolvedTitle' (${realDurationMs / 1000}s) on V1"
            )
        }
    }

    fun importPipOverlay(uri: Uri?, opticalPreset: String? = null, blendMode: BlendModePreset = BlendModePreset.SCREEN) {
        if (uri != null) {
            runCatching {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        val newPipId = java.util.UUID.randomUUID().toString()
        updateProject { proj ->
            val pip = OverlayPipClip(
                id = newPipId,
                title = opticalPreset ?: (uri?.let { queryDisplayName(it) } ?: "Overlay"),
                mediaUri = uri?.toString() ?: "",
                opticalFxPreset = opticalPreset,
                startMs = _uiState.value.playheadMs,
                blendMode = blendMode
            )
            proj.copy(
                pipClips = proj.pipClips + pip,
                tracks = proj.tracks.copy(o1Visible = true)
            )
        }
        _uiState.update { it.copy(selectedPipId = newPipId, statusBanner = "Added Overlay to Track O1") }
    }

    fun updateSelectedClip(transform: (VideoClip) -> VideoClip) {
        val idx = playheadClipIndexAndNormT().first
        updateProject { proj ->
            if (idx !in proj.videoClips.indices) return@updateProject proj
            val updated = proj.videoClips.toMutableList()
            updated[idx] = transform(updated[idx])
            proj.copy(videoClips = updated)
        }
    }

    fun currentClipNormalizedTime(): Float = playheadClipIndexAndNormT().second

    private fun playheadClipIndexAndNormT(): Pair<Int, Float> {
        val clips = _uiState.value.project.videoClips
        if (clips.isEmpty()) return 0 to 0f
        val ph = _uiState.value.playheadMs
        var acc = 0L
        clips.forEachIndexed { idx, c ->
            val dur = c.activeDurationMs.coerceAtLeast(300L)
            if (ph in acc..(acc + dur)) {
                return idx to ((ph - acc).toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
            acc += dur
        }
        return clips.lastIndex to 1f
    }

    fun toggleKeyframeAtPlayhead() {
        val (targetIdx, normT) = playheadClipIndexAndNormT()
        var added = false
        updateProject { proj ->
            val nextClips = proj.videoClips.mapIndexed { i, clip ->
                if (i != targetIdx) clip else {
                    val existing = clip.keyframes.indexOfFirst { kotlin.math.abs(it.normalizedTime - normT) < 0.04f }
                    val nextList = if (existing >= 0) {
                        clip.keyframes.toMutableList().apply { removeAt(existing) }
                    } else {
                        added = true
                        val newPt = if (clip.keyframes.isNotEmpty()) {
                            CurveEvaluators.interpolateKeyframes(clip.keyframes, normT).copy(normalizedTime = normT)
                        } else {
                            KeyframePoint(
                                normalizedTime = normT,
                                x = (0.5f + clip.panX / 560f).coerceIn(0f, 1f),
                                y = (0.5f + clip.panY / 560f).coerceIn(0f, 1f),
                                scale = clip.zoomScale,
                                rotationDeg = clip.rotationDeg
                            )
                        }
                        (clip.keyframes + newPt).sortedBy { it.normalizedTime }
                    }
                    clip.copy(keyframes = nextList)
                }
            }
            proj.copy(videoClips = nextClips)
        }
        _uiState.update {
            it.copy(
                selectedVideoIndex = targetIdx,
                statusBanner = if (added) "Added Keyframe ◆ at ${(normT * 100).toInt()}%" else "Removed Keyframe ◆"
            )
        }
    }

    fun updateKeyframeAtPlayhead(scale: Float, rotationDeg: Float, x: Float, y: Float, opacity: Float) {
        val (targetIdx, normT) = playheadClipIndexAndNormT()
        updateProject { proj ->
            val nextClips = proj.videoClips.mapIndexed { i, clip ->
                if (i != targetIdx) clip else {
                    val idx = clip.keyframes.indexOfFirst { kotlin.math.abs(it.normalizedTime - normT) < 0.05f }
                    val pt = KeyframePoint(
                        normalizedTime = normT,
                        x = x,
                        y = y,
                        scale = scale,
                        rotationDeg = rotationDeg,
                        opacity = opacity
                    )
                    val nextList = if (idx >= 0) {
                        clip.keyframes.toMutableList().apply { this[idx] = pt }
                    } else {
                        (clip.keyframes + pt).sortedBy { it.normalizedTime }
                    }
                    clip.copy(keyframes = nextList)
                }
            }
            proj.copy(videoClips = nextClips)
        }
    }

    fun applyKeyframePreset(preset: String) {
        val (targetIdx, _) = playheadClipIndexAndNormT()
        val points = when (preset) {
            "ZOOM_IN" -> listOf(
                KeyframePoint(normalizedTime = 0f, scale = 1.0f),
                KeyframePoint(normalizedTime = 1f, scale = 1.45f)
            )
            "ZOOM_OUT" -> listOf(
                KeyframePoint(normalizedTime = 0f, scale = 1.45f),
                KeyframePoint(normalizedTime = 1f, scale = 1.0f)
            )
            "PAN_LR" -> listOf(
                KeyframePoint(normalizedTime = 0f, x = 0.25f, scale = 1.25f),
                KeyframePoint(normalizedTime = 1f, x = 0.75f, scale = 1.25f)
            )
            else -> listOf(
                KeyframePoint(normalizedTime = 0f, scale = 0.72f, rotationDeg = -16f),
                KeyframePoint(normalizedTime = 0.5f, scale = 1.15f, rotationDeg = 0f),
                KeyframePoint(normalizedTime = 1f, scale = 1.0f, rotationDeg = 0f)
            )
        }
        updateProject { proj ->
            proj.copy(
                videoClips = proj.videoClips.mapIndexed { i, c ->
                    if (i == targetIdx) c.copy(keyframes = points) else c
                }
            )
        }
        _uiState.update { it.copy(selectedVideoIndex = targetIdx, statusBanner = "Applied Keyframe Motion: $preset") }
    }

    fun updateKeyframeCurve(curveName: String) {
        val (targetIdx, _) = playheadClipIndexAndNormT()
        updateProject { proj ->
            val nextClips = proj.videoClips.mapIndexed { i, c ->
                if (i != targetIdx) c else c.copy(keyframes = c.keyframes.map { it.copy(easingCurve = curveName) })
            }
            proj.copy(videoClips = nextClips)
        }
        _uiState.update { it.copy(statusBanner = "Keyframe Graph Curve: $curveName") }
    }

    fun clearAllKeyframes() {
        val (targetIdx, _) = playheadClipIndexAndNormT()
        updateProject { proj ->
            proj.copy(
                videoClips = proj.videoClips.mapIndexed { i, c ->
                    if (i == targetIdx) c.copy(keyframes = emptyList()) else c
                }
            )
        }
        _uiState.update { it.copy(statusBanner = "Cleared clip keyframes") }
    }

    fun splitSelectedClipAtPlayhead() {
        val proj = _uiState.value.project
        if (proj.tracks.v1Locked) {
            _uiState.update { it.copy(statusBanner = "V1 track is locked") }
            return
        }
        val (idx, rawNormT) = playheadClipIndexAndNormT()
        if (idx !in proj.videoClips.indices) return
        val clip = proj.videoClips[idx]
        if (clip.activeDurationMs < 800L) return
        val normT = rawNormT.coerceIn(0.15f, 0.85f)
        val splitPoint = clip.trimStartMs + ((clip.trimEndMs - clip.trimStartMs) * normT).toLong()
        val first = clip.copy(title = "${clip.title} A", trimEndMs = splitPoint)
        val second = clip.copy(id = java.util.UUID.randomUUID().toString(), title = "${clip.title} B", trimStartMs = splitPoint)
        updateProject {
            val list = it.videoClips.toMutableList()
            list[idx] = first
            list.add(idx + 1, second)
            it.copy(videoClips = list)
        }
    }

    fun addStickerOverlay(stickerText: String) {
        val atMs = _uiState.value.playheadMs
        updateProject { proj ->
            proj.copy(
                pipClips = proj.pipClips + OverlayPipClip(
                    title = stickerText,
                    opticalFxPreset = "STICKER:$stickerText",
                    startMs = atMs,
                    durationMs = 3000L,
                    blendMode = BlendModePreset.NORMAL,
                    opacity = 1f,
                    posX = 0.5f,
                    posY = 0.35f,
                    scale = 0.6f
                ),
                tracks = proj.tracks.copy(o1Visible = true)
            )
        }
        _uiState.update { it.copy(statusBanner = "Added Sticker '$stickerText' at ${atMs}ms") }
    }

    fun duplicatePipClip(pipId: String) {
        updateProject { proj ->
            val src = proj.pipClips.firstOrNull { it.id == pipId } ?: return@updateProject proj
            val copy = src.copy(
                id = java.util.UUID.randomUUID().toString(),
                title = "${src.title} Copy",
                posX = (src.posX + 0.06f).coerceAtMost(0.9f),
                posY = (src.posY + 0.06f).coerceAtMost(0.9f)
            )
            proj.copy(pipClips = proj.pipClips + copy)
        }
        _uiState.update { it.copy(statusBanner = "Duplicated Overlay on Track O1") }
    }

    fun replacePipClip(pipId: String, uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val name = queryDisplayName(uri) ?: "Replaced Overlay"
        updateProject { proj ->
            proj.copy(
                pipClips = proj.pipClips.map { p ->
                    if (p.id == pipId) p.copy(title = name, mediaUri = uri.toString(), opticalFxPreset = null) else p
                }
            )
        }
        _uiState.update { it.copy(statusBanner = "Replaced Overlay media with '$name'") }
    }

    fun selectPip(pipId: String?) {
        _uiState.update { it.copy(selectedPipId = pipId) }
    }

    fun trimClipStart(clipIndex: Int, deltaMs: Long) {
        updateProject(recordUndo = false) { proj ->
            val clips = proj.videoClips.toMutableList()
            if (clipIndex !in clips.indices) return@updateProject proj
            val c = clips[clipIndex]
            val nextStart = (c.trimStartMs + deltaMs).coerceIn(0L, (c.trimEndMs - 300L).coerceAtLeast(0L))
            clips[clipIndex] = c.copy(trimStartMs = nextStart)
            proj.copy(videoClips = clips)
        }
    }

    fun trimClipEnd(clipIndex: Int, deltaMs: Long) {
        updateProject(recordUndo = false) { proj ->
            val clips = proj.videoClips.toMutableList()
            if (clipIndex !in clips.indices) return@updateProject proj
            val c = clips[clipIndex]
            val maxEnd = c.trimEndMs.coerceAtLeast(c.trimEndMs + 5000L)
            val nextEnd = (c.trimEndMs + deltaMs).coerceIn(c.trimStartMs + 300L, maxEnd)
            clips[clipIndex] = c.copy(trimEndMs = nextEnd)
            proj.copy(videoClips = clips)
        }
    }

    fun moveCaptionTiming(capId: String, deltaMs: Long) {
        updateProject(recordUndo = false) { proj ->
            val maxStart = (proj.totalDurationMs - 400L).coerceAtLeast(0L)
            proj.copy(
                captionClips = proj.captionClips.map { cap ->
                    if (cap.id == capId) {
                        val raw = (cap.startMs + deltaMs).coerceIn(0L, maxStart); val ph = _uiState.value.playheadMs; val snapped = when { kotlin.math.abs(raw - ph) <= 140L -> ph; raw <= 100L -> 0L; else -> raw }; cap.copy(startMs = snapped)
                    } else cap
                }
            )
        }
    }

    fun movePipTiming(pipId: String, deltaMs: Long) {
        updateProject(recordUndo = false) { proj ->
            val maxStart = (proj.totalDurationMs - 400L).coerceAtLeast(0L)
            proj.copy(
                pipClips = proj.pipClips.map { pip ->
                    if (pip.id == pipId) {
                        val raw = (pip.startMs + deltaMs).coerceIn(0L, maxStart); val ph = _uiState.value.playheadMs; val snapped = when { kotlin.math.abs(raw - ph) <= 140L -> ph; raw <= 100L -> 0L; else -> raw }; pip.copy(startMs = snapped)
                    } else pip
                }
            )
        }
    }

    fun deletePipClip(pipId: String) {
        updateProject { proj ->
            proj.copy(pipClips = proj.pipClips.filterNot { it.id == pipId })
        }
        _uiState.update { st ->
            st.copy(
                selectedPipId = if (st.selectedPipId == pipId) null else st.selectedPipId,
                statusBanner = "Deleted Overlay"
            )
        }
    }

    fun transformPipOverlay(pipId: String, zoomFactor: Float, panDxPx: Float, panDyPx: Float, rotDeg: Float) {
        updateProject(recordUndo = false) { proj ->
            proj.copy(
                pipClips = proj.pipClips.map { pip ->
                    if (pip.id == pipId) {
                        pip.copy(
                            posX = (pip.posX + panDxPx / 360f).coerceIn(-1.0f, 2.0f),
                            posY = (pip.posY + panDyPx / 640f).coerceIn(-1.0f, 2.0f),
                            scale = (pip.scale * zoomFactor).coerceIn(0.1f, 4.0f),
                            rotationDeg = (pip.rotationDeg + rotDeg) % 360f
                        )
                    } else pip
                }
            )
        }
    }

    fun movePipPosition(pipId: String, dxNorm: Float, dyNorm: Float) {
        updateProject(recordUndo = false) { proj ->
            proj.copy(
                pipClips = proj.pipClips.map { p ->
                    if (p.id == pipId) {
                        p.copy(
                            posX = (p.posX + dxNorm).coerceIn(0.1f, 0.9f),
                            posY = (p.posY + dyNorm).coerceIn(0.1f, 0.9f)
                        )
                    } else p
                }
            )
        }
    }

    fun freezeFrameAtPlayhead() {
        val proj = _uiState.value.project
        if (proj.tracks.v1Locked) return
        val idx = playheadClipIndexAndNormT().first
        val clip = proj.videoClips.getOrNull(idx) ?: return
        if (clip.mediaUri.isBlank()) {
            _uiState.update { it.copy(statusBanner = "Import a video first to Freeze Frame") }
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Capturing Freeze Frame...") }
            val retriever = MediaMetadataRetriever()
            val outFile = java.io.File(getApplication<Application>().cacheDir, "freeze_${System.currentTimeMillis()}.png")
            val ok = try {
                retriever.setDataSource(getApplication<Application>(), Uri.parse(clip.mediaUri))
                val frame = retriever.getFrameAtTime(_uiState.value.playheadMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    java.io.FileOutputStream(outFile).use { fos ->
                        frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, fos)
                    }
                    frame.recycle()
                    true
                } else false
            } catch (_: Exception) {
                false
            } finally {
                runCatching { retriever.release() }
            }
            if (ok) {
                val freezeClip = VideoClip(
                    title = "Freeze 2.5s",
                    mediaUri = Uri.fromFile(outFile).toString(),
                    isAiGeneratedBroll = true,
                    sourceDurationMs = 2500L,
                    trimStartMs = 0L,
                    trimEndMs = 2500L
                )
                updateProject { p ->
                    val mutable = p.videoClips.toMutableList()
                    mutable.add((idx + 1).coerceAtMost(mutable.size), freezeClip)
                    p.copy(videoClips = mutable)
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Inserted 2.5s Freeze Frame on V1") }
            } else {
                _uiState.update { it.copy(isBusy = false, statusBanner = "Could not capture frame for Freeze") }
            }
        }
    }

    fun moveSelectedClipToOverlay(blendMode: BlendModePreset = BlendModePreset.NORMAL) {
        val proj = _uiState.value.project
        val idx = playheadClipIndexAndNormT().first
        val clip = proj.videoClips.getOrNull(idx) ?: return
        if (clip.mediaUri.isBlank()) {
            _uiState.update { it.copy(statusBanner = "Import a clip first to move to Overlay") }
            return
        }
        val atMs = _uiState.value.playheadMs
        val pip = OverlayPipClip(
            title = "Overlay: ${clip.title}",
            mediaUri = clip.mediaUri,
            startMs = atMs,
            durationMs = clip.activeDurationMs.coerceAtLeast(1500L),
            blendMode = blendMode,
            opacity = 0.95f,
            posX = 0.5f,
            posY = 0.5f,
            scale = 0.55f
        )
        updateProject { p ->
            val remaining = if (p.videoClips.size > 1) {
                p.videoClips.filterIndexed { i, _ -> i != idx }
            } else p.videoClips
            p.copy(
                videoClips = remaining,
                pipClips = p.pipClips + pip,
                tracks = p.tracks.copy(o1Visible = true)
            )
        }
        _uiState.update {
            it.copy(
                selectedVideoIndex = 0,
                statusBanner = "Moved '${clip.title}' to Overlay Track O1"
            )
        }
    }

    fun duplicateSelectedClip() {
        val idx = playheadClipIndexAndNormT().first
        val clips = _uiState.value.project.videoClips
        if (idx !in clips.indices) return
        val copy = clips[idx].copy(id = java.util.UUID.randomUUID().toString(), title = "${clips[idx].title} Copy")
        updateProject { p ->
            val list = p.videoClips.toMutableList()
            list.add(idx + 1, copy)
            p.copy(videoClips = list)
        }
        _uiState.update { it.copy(selectedVideoIndex = idx + 1, statusBanner = "Duplicated clip") }
    }

    fun setFaceSwapSourcePhoto(uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        updateSelectedClip {
            it.copy(faceSwap = it.faceSwap.copy(enabled = true, sourceFaceUri = uri.toString()))
        }
        _uiState.update { it.copy(statusBanner = "Face Swap photo locked with ML Kit tracking") }
    }

    fun toggleMagicEraserMode() {
        val next = !_uiState.value.isMagicEraserMode
        _uiState.update {
            it.copy(
                isMagicEraserMode = next,
                isMotionTrackerPicking = false,
                isMaskEditMode = false,
                statusBanner = if (next) "Magic Brush ON: Paint over any object on preview to erase" else "Magic Brush OFF"
            )
        }
    }

    fun addMagicEraserPoint(normX: Float, normY: Float) {
        val idx = playheadClipIndexAndNormT().first
        updateProject(recordUndo = false) { proj ->
            proj.copy(
                videoClips = proj.videoClips.mapIndexed { i, clip ->
                    if (i != idx) clip else {
                        val last = clip.magicEraserPoints.lastOrNull()
                        val tooClose = last != null &&
                            kotlin.math.hypot((last.x - normX).toDouble(), (last.y - normY).toDouble()) < 0.025
                        if (tooClose || clip.magicEraserPoints.size >= 24) clip
                        else clip.copy(
                            magicEraserPoints = clip.magicEraserPoints + BrushEraserPoint(
                                x = normX.coerceIn(0.02f, 0.98f),
                                y = normY.coerceIn(0.02f, 0.98f)
                            )
                        )
                    }
                }
            )
        }
    }

    fun clearMagicEraser() {
        updateSelectedClip { it.copy(magicEraserPoints = emptyList()) }
        _uiState.update { it.copy(isMagicEraserMode = false, statusBanner = "Cleared Magic Brush strokes") }
    }

    fun moveWatermarkBox(dxNorm: Float, dyNorm: Float) {
        updateSelectedClip { clip ->
            val wm = clip.watermarkRemover
            clip.copy(
                watermarkRemover = wm.copy(
                    posX = (wm.posX + dxNorm).coerceIn(0.08f, 0.92f),
                    posY = (wm.posY + dyNorm).coerceIn(0.06f, 0.94f)
                )
            )
        }
    }

    fun transformSelectedClip(zoomFactor: Float, panDxPx: Float, panDyPx: Float) {
        val (idx, normT) = playheadClipIndexAndNormT()
        updateProject(recordUndo = false) { proj ->
            proj.copy(
                videoClips = proj.videoClips.mapIndexed { i, clip ->
                    if (i != idx) clip else if (clip.keyframes.isNotEmpty()) {
                        val existingIdx = clip.keyframes.indexOfFirst { kotlin.math.abs(it.normalizedTime - normT) < 0.05f }
                        val base = if (existingIdx >= 0) {
                            clip.keyframes[existingIdx]
                        } else {
                            CurveEvaluators.interpolateKeyframes(clip.keyframes, normT)
                        }
                        val nextPt = base.copy(
                            normalizedTime = if (existingIdx >= 0) base.normalizedTime else normT,
                            scale = (base.scale * zoomFactor).coerceIn(0.05f, 10.0f),
                            x = (base.x + panDxPx / 560f).coerceIn(-1.5f, 2.5f),
                            y = (base.y + panDyPx / 560f).coerceIn(-1.5f, 2.5f)
                        )
                        val nextList = if (existingIdx >= 0) {
                            clip.keyframes.toMutableList().apply { this[existingIdx] = nextPt }
                        } else {
                            (clip.keyframes + nextPt).sortedBy { it.normalizedTime }
                        }
                        clip.copy(keyframes = nextList)
                    } else {
                        clip.copy(
                            zoomScale = (clip.zoomScale * zoomFactor).coerceIn(0.05f, 10.0f),
                            panX = (clip.panX + panDxPx).coerceIn(-1500f, 1500f),
                            panY = (clip.panY + panDyPx).coerceIn(-2000f, 2000f)
                        )
                    }
                }
            )
        }
    }

    fun resetClipTransform() {
        updateSelectedClip { clip ->
            clip.copy(zoomScale = 1f, panX = 0f, panY = 0f, rotationDeg = 0f)
        }
        _uiState.update { it.copy(statusBanner = "Reset Zoom & Pan to 1x") }
    }

    fun rotateSelectedClip90() {
        updateSelectedClip { clip ->
            clip.copy(rotationDeg = (clip.rotationDeg + 90f) % 360f)
        }
        _uiState.update { it.copy(statusBanner = "Rotated clip 90°") }
    }

    fun replaceSelectedClip(uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val name = queryDisplayName(uri) ?: "Replaced Clip"
        val durMs = queryMediaDurationMs(uri).coerceAtLeast(3000L)
        updateSelectedClip { clip ->
            clip.copy(
                title = name,
                mediaUri = uri.toString(),
                isAiGeneratedBroll = false,
                sourceDurationMs = durMs,
                trimStartMs = 0L,
                trimEndMs = durMs
            )
        }
        _uiState.update { it.copy(statusBanner = "Replaced clip with '$name'") }
    }

    fun flipSelectedClip() {
        updateSelectedClip { it.copy(flipHorizontal = !it.flipHorizontal) }
        _uiState.update { it.copy(statusBanner = "Flipped clip horizontally") }
    }

    fun rippleDeleteSelectedClip() {
        val selPip = _uiState.value.selectedPipId
        val proj = _uiState.value.project
        if (selPip != null && proj.pipClips.any { it.id == selPip }) {
            deletePipClip(selPip)
            return
        }
        if (proj.tracks.v1Locked || proj.videoClips.size <= 1) return
        val idx = playheadClipIndexAndNormT().first
        updateProject { p ->
            p.copy(videoClips = p.videoClips.filterIndexed { i, _ -> i != idx })
        }
        _uiState.update { it.copy(selectedVideoIndex = 0, statusBanner = "Magnetic ripple deleted clip") }
    }

    fun moveSelectedClip(delta: Int) {
        val idx = playheadClipIndexAndNormT().first
        val clips = _uiState.value.project.videoClips
        val target = idx + delta
        if (idx !in clips.indices || target !in clips.indices) return
        updateProject { p ->
            val mutable = p.videoClips.toMutableList()
            val item = mutable.removeAt(idx)
            mutable.add(target, item)
            p.copy(videoClips = mutable)
        }
        _uiState.update { it.copy(selectedVideoIndex = target) }
    }

    fun addOrUpdateCaption(
        text: String,
        anim: MotionAnimation3D,
        trackedToClipId: String? = null,
        stylePreset: CaptionStylePreset = CaptionStylePreset.VIRAL_HORMOZI,
        colorHex: Long = 0xFFF8FAFC,
        accentHex: Long = 0xFFF59E0B,
        fontSizeSp: Float = 22f,
        posY: Float = 0.8f,
        fontStyle: CaptionFontStyle = CaptionFontStyle.SANS_BOLD
    ) {
        updateProject { proj ->
            val cap = CaptionClip(
                text = text.ifBlank { "KB STUDIO" },
                startMs = _uiState.value.playheadMs,
                durationMs = 3000L,
                animation3D = anim,
                stylePreset = stylePreset,
                fontStyle = fontStyle,
                posY = posY,
                fontSizeSp = fontSizeSp,
                colorHex = colorHex,
                accentHex = accentHex,
                trackedToClipId = trackedToClipId
            )
            proj.copy(captionClips = proj.captionClips + cap)
        }
    }

    fun applyCaptionDesignToAll(
        stylePreset: CaptionStylePreset,
        anim: MotionAnimation3D,
        colorHex: Long,
        accentHex: Long,
        fontSizeSp: Float,
        posY: Float,
        fontStyle: CaptionFontStyle = CaptionFontStyle.SANS_BOLD
    ) {
        updateProject { proj ->
            proj.copy(
                captionClips = proj.captionClips.map {
                    it.copy(
                        stylePreset = stylePreset,
                        fontStyle = fontStyle,
                        animation3D = anim,
                        colorHex = colorHex,
                        accentHex = accentHex,
                        fontSizeSp = fontSizeSp,
                        posY = posY
                    )
                }
            )
        }
        _uiState.update { it.copy(statusBanner = "Updated design for all ${uiState.value.project.captionClips.size} captions") }
    }

    fun deleteCaption(captionId: String) {
        updateProject { proj ->
            proj.copy(captionClips = proj.captionClips.filterNot { it.id == captionId })
        }
    }

    fun clearAllCaptions() {
        updateProject { proj -> proj.copy(captionClips = emptyList()) }
        _uiState.update { it.copy(statusBanner = "Cleared all captions") }
    }

    fun runAutoCaptions(
        language: CaptionLanguage,
        stylePreset: CaptionStylePreset,
        colorHex: Long,
        accentHex: Long,
        fontSizeSp: Float,
        posY: Float,
        customLanguageName: String = ""
    ) {
        if (!trySpendProCoins(5)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Generating ${language.label} Auto-Captions...") }
            val res = geminiEngine.generateAutoCaptions(
                project = _uiState.value.project,
                language = language,
                style = stylePreset,
                colorHex = colorHex,
                accentHex = accentHex,
                fontSizeSp = fontSizeSp,
                posY = posY,
                customLanguageName = customLanguageName
            )
            res.onSuccess { caps ->
                updateProject { proj ->
                    proj.copy(
                        captionClips = caps,
                        tracks = proj.tracks.copy(t1Visible = true)
                    )
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusBanner = "Generated ${caps.size} ${language.label} captions (${stylePreset.label})"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusBanner = err.message ?: "Auto-Caption failed"
                    )
                }
            }
        }
    }

    fun applyHomeTemplate(templateId: String) {
        updateProject { proj ->
            val clips = proj.videoClips.mapIndexed { idx, clip ->
                when (templateId) {
                    "REELS_BEAT" -> clip.copy(
                        speedRamp = SpeedRampPreset.HERO_DIP,
                        animIn = ClipAnimPreset.IN_BEAT_BOUNCE, videoFx = VideoFxPreset.ZOOM_PULSE, videoFxIntensity = 1.35f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Reels Beat",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.WHIP_PAN else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.18f, saturation = 1.18f, lutName = "Teal & Orange Pro")
                    )
                    "THUNDER_ACTION" -> clip.copy(
                        speedRamp = SpeedRampPreset.FLASH_IN,
                        animIn = ClipAnimPreset.IN_SHAKE_1, videoFx = VideoFxPreset.ACTION_FLASH, videoFxIntensity = 1.6f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Thunder Storm",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.FLASH_CUT else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.24f, saturation = 0.92f, warmth = -0.18f, vignette = 0.35f, lutName = "Storm Moody")
                    )
                    "FLOWER_DREAM" -> clip.copy(
                        speedRamp = SpeedRampPreset.BULLET_TIME,
                        animIn = ClipAnimPreset.IN_GLOW_FADE, videoFx = VideoFxPreset.SOFT_GLOW, videoFxIntensity = 1.4f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Flower Petals",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.CROSS_DISSOLVE else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(brightness = 0.06f, saturation = 1.16f, warmth = 0.14f, beautyGlow = 0.30f, lutName = "Sakura Bloom")
                    )
                    "BLINK_STAR" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_POP, videoFx = VideoFxPreset.GOLD_SEQUINS, videoFxIntensity = 1.45f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Blink Star",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.CROSS_DISSOLVE else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.14f, vibrance = 0.25f, beautyGlow = 0.24f, lutName = "Starlight")
                    )
                    "BOLLYWOOD_SLO" -> clip.copy(
                        speedRamp = SpeedRampPreset.BULLET_TIME,
                        animIn = ClipAnimPreset.IN_ZOOM_1, videoFx = VideoFxPreset.GOLDEN_HOUR, videoFxIntensity = 1.3f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Golden Slow-Mo",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.CROSS_DISSOLVE else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.15f, warmth = 0.22f, saturation = 1.18f, shadows = 0.14f, lutName = "Bollywood Gold")
                    )
                    "WEDDING_GOLD" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_FADE, videoFx = VideoFxPreset.PEARL_GLOW, videoFxIntensity = 1.3f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Wedding Sparkle",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.CROSS_DISSOLVE else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(warmth = 0.18f, highlights = 0.10f, beautyGlow = 0.28f, lutName = "Royal Wedding")
                    )
                    "GAMING_PHONK" -> clip.copy(
                        speedRamp = SpeedRampPreset.HERO_DIP,
                        animIn = ClipAnimPreset.IN_SUDDEN_IMPACT, videoFx = VideoFxPreset.EARTHQUAKE, videoFxIntensity = 1.5f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Phonk Shake",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.WHIP_PAN else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.32f, saturation = 1.28f, vignette = 0.42f, lutName = "Dark Gotham")
                    )
                    "VLOG_FILM" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_FADE, videoFx = VideoFxPreset.CINEMA_GRAIN, videoFxIntensity = 1.2f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Vlog Film",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.CROSS_DISSOLVE else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(warmth = 0.16f, vignette = 0.25f, lutName = "Kodak 2383")
                    )
                    "PODCAST_PRO" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        videoFx = VideoFxPreset.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.10f, beautyGlow = 0.18f, lutName = "Studio Crisp")
                    )
                    "VHS_90S" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_SCANLINE, videoFx = VideoFxPreset.VHS_RETRO, videoFxIntensity = 1.4f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "90s VHS",
                        colorGrade = clip.colorGrade.copy(contrast = 1.12f, saturation = 0.88f, vignette = 0.30f, lutName = "Retro 1998")
                    )
                    "WINTER_SNOW" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_FADE, videoFx = VideoFxPreset.BOUNCING_SNOW, videoFxIntensity = 1.35f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Snowfall",
                        colorGrade = clip.colorGrade.copy(warmth = -0.22f, brightness = 0.05f, contrast = 1.10f, lutName = "Arctic Chill")
                    )
                    "FIRE_ENERGY" -> clip.copy(
                        speedRamp = SpeedRampPreset.BUILD_UP,
                        animIn = ClipAnimPreset.IN_POP, videoFx = VideoFxPreset.NITRO_BOOST, videoFxIntensity = 1.45f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Fire Embers",
                        colorGrade = clip.colorGrade.copy(warmth = 0.28f, contrast = 1.22f, saturation = 1.24f, lutName = "Lava Blaze")
                    )
                    "LOVE_HEARTS" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_GLOW_FADE, videoFx = VideoFxPreset.HEARTBEAT, videoFxIntensity = 1.3f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Romantic Hearts",
                        colorGrade = clip.colorGrade.copy(warmth = 0.15f, saturation = 1.18f, beautyGlow = 0.25f, lutName = "Rose Pink")
                    )
                    "PRISM_AURORA" -> clip.copy(
                        speedRamp = SpeedRampPreset.FLASH_IN,
                        animIn = ClipAnimPreset.IN_ZOOM_FADE, videoFx = VideoFxPreset.PRISM_SHIMMER, videoFxIntensity = 1.35f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Prism Rainbow",
                        colorGrade = clip.colorGrade.copy(vibrance = 0.35f, contrast = 1.16f, lutName = "Aurora")
                    )
                    "NOIR_CINEMA" -> clip.copy(
                        speedRamp = SpeedRampPreset.CONSTANT,
                        animIn = ClipAnimPreset.IN_FADE, videoFx = VideoFxPreset.NOIR_SHADOW, videoFxIntensity = 1.3f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Noir Cinema",
                        colorGrade = clip.colorGrade.copy(saturation = 0.15f, contrast = 1.34f, vignette = 0.45f, lutName = "Noir B&W")
                    )
                    else -> clip.copy(
                        speedRamp = SpeedRampPreset.FLASH_IN,
                        animIn = ClipAnimPreset.IN_POP, videoFx = VideoFxPreset.CHROMATIC, videoFxIntensity = 1.3f, videoFxStartMs = 0L, videoFxEndMs = -1L, videoFxLabel = "Cyber Neon",
                        transitionOut = if (idx < proj.videoClips.lastIndex) TransitionType.FLASH_CUT else TransitionType.NONE,
                        colorGrade = clip.colorGrade.copy(contrast = 1.22f, saturation = 1.25f, lutName = "Cyber Neon")
                    )
                }
            }
            val audio = if (templateId == "PODCAST_PRO") {
                val base = proj.audioClips.ifEmpty { listOf(AudioClip(title = "Podcast Bus")) }
                base.map { it.copy(voiceEnhanceEnabled = true, noiseGateEnabled = true, windFilterEnabled = true) }
            } else proj.audioClips
            proj.copy(videoClips = clips, audioClips = audio)
        }
        _uiState.update { it.copy(statusBanner = "Applied Studio Template: $templateId") }
    }

    fun extractAudioFromSelectedClip() {
        val targetIdx = playheadClipIndexAndNormT().first
        val clip = _uiState.value.project.videoClips.getOrNull(targetIdx) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Extracting audio stream...") }
            val res = audioExtractor.extractAudioToM4a(clip.mediaUri, clip.title)
            res.onSuccess { (uriStr, durMs) ->
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = "${clip.title} Audio",
                            mediaUri = uriStr,
                            durationMs = durMs
                        )
                    )
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Extracted audio to Track A1") }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "No embedded audio stream") }
            }
        }
    }

    fun refreshDenoiseBackendStatus() {
        _uiState.update { it.copy(denoiseBackendLabel = denoiseEngine.activeBackend().label) }
    }

    fun toggleAbNoiseBypass(bypass: Boolean) {
        _uiState.update { it.copy(abBypassNoiseReduction = bypass) }
    }

    fun updateNoiseReductionStrength(strength: Float) {
        val clamped = strength.coerceIn(0f, 1f)
        updateProject(recordUndo = false) { proj ->
            val list = if (proj.audioClips.isEmpty()) {
                listOf(AudioClip(title = "Master Audio Bus", noiseGateEnabled = true, noiseReductionStrength = clamped))
            } else {
                proj.audioClips.map { it.copy(noiseReductionStrength = clamped) }
            }
            proj.copy(audioClips = list)
        }
    }

    fun runOfflineNoiseReduction(strength: Float, applyEntireVideo: Boolean) {
        val proj = _uiState.value.project
        val targetUri = proj.audioClips.lastOrNull { it.mediaUri.isNotBlank() && !it.title.startsWith("SFX:") }?.mediaUri
            ?: proj.videoClips.getOrNull(_uiState.value.selectedVideoIndex)?.mediaUri
            ?: proj.videoClips.firstOrNull { it.mediaUri.isNotBlank() }?.mediaUri
        if (targetUri.isNullOrBlank()) {
            _uiState.update { it.copy(statusBanner = "Import audio or video first") }
            return
        }
        viewModelScope.launch {
            val backend = denoiseEngine.activeBackend()
            _uiState.update { it.copy(isBusy = true, denoiseBackendLabel = backend.label, statusBanner = "Processing noise reduction (${backend.label})...") }
            val res = denoiseEngine.denoiseMediaAudioToWav(targetUri, strength)
            res.onSuccess { (wavUri, durMs, usedBackend) ->
                updateProject { p ->
                    val updatedVideos = if (applyEntireVideo) p.videoClips.map { it.copy(muted = true) } else p.videoClips
                    p.copy(
                        videoClips = updatedVideos,
                        audioClips = p.audioClips + AudioClip(
                            title = "Cleaned Audio (${(strength * 100).toInt()}%)",
                            mediaUri = wavUri,
                            startMs = 0L,
                            durationMs = durMs,
                            noiseGateEnabled = false,
                            noiseReductionStrength = strength
                        ),
                        tracks = p.tracks.copy(a1Muted = false)
                    )
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Denoised track added to A1 (${usedBackend.label})") }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "Denoise failed") }
            }
        }
    }

    fun updateEchoRemoval(enabled: Boolean) {
        updateProject { proj ->
            val list = if (proj.audioClips.isEmpty()) {
                listOf(AudioClip(title = "Master Audio Bus", echoRemovalEnabled = enabled))
            } else {
                proj.audioClips.map { it.copy(echoRemovalEnabled = enabled) }
            }
            proj.copy(audioClips = list)
        }
    }

    fun updateAudioDsp(windFilter: Boolean, noiseGate: Boolean, voiceEnhance: Boolean, vocalMode: VocalIsolationMode) {
        updateProject { proj ->
            val list = if (proj.audioClips.isEmpty()) {
                listOf(
                    AudioClip(
                        title = "Master Audio Bus",
                        windFilterEnabled = windFilter,
                        noiseGateEnabled = noiseGate,
                        voiceEnhanceEnabled = voiceEnhance,
                        vocalMode = vocalMode
                    )
                )
            } else {
                proj.audioClips.map {
                    it.copy(
                        windFilterEnabled = windFilter,
                        noiseGateEnabled = noiseGate,
                        voiceEnhanceEnabled = voiceEnhance,
                        vocalMode = vocalMode
                    )
                }
            }
            proj.copy(audioClips = list)
        }
    }

    fun addCapCutSfxVariant(label: String, preset: CapCutSfxPreset, pitchMul: Float, gainMul: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Adding SFX: $label...") }
            val res = sfxEngine.generateSfxWav(preset, pitchMul, gainMul, label)
            res.onSuccess { (uriStr, durMs) ->
                val atMs = _uiState.value.playheadMs
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = "SFX: $label",
                            mediaUri = uriStr,
                            startMs = atMs,
                            durationMs = durMs
                        ),
                        tracks = proj.tracks.copy(a1Muted = false)
                    )
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Added SFX: $label") }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "SFX error") }
            }
        }
    }

    fun addCapCutSfx(preset: CapCutSfxPreset) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Adding SFX: ${preset.label}...") }
            val res = sfxEngine.generateSfxWav(preset)
            res.onSuccess { (uriStr, durMs) ->
                val atMs = _uiState.value.playheadMs
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = "SFX: ${preset.label}",
                            mediaUri = uriStr,
                            startMs = atMs,
                            durationMs = durMs
                        ),
                        tracks = proj.tracks.copy(a1Muted = false)
                    )
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Added SFX '${preset.label}' at ${atMs}ms on A1") }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "SFX error") }
            }
        }
    }

    fun importOnlineAudioUrl(url: String) {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            _uiState.update { it.copy(statusBanner = "Enter a valid https:// audio/SFX URL") }
            return
        }
        val atMs = _uiState.value.playheadMs
        updateProject { proj ->
            proj.copy(
                audioClips = proj.audioClips + AudioClip(
                    title = "Online: ${clean.substringAfterLast('/').take(18).ifBlank { "Audio" }}",
                    mediaUri = clean,
                    startMs = atMs,
                    durationMs = 6000L
                ),
                tracks = proj.tracks.copy(a1Muted = false)
            )
        }
        _uiState.update { it.copy(statusBanner = "Added Online Audio/SFX to Track A1") }
    }

    fun importLocalAudioUri(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val name = queryDisplayName(uri) ?: "Music Track"
        val atMs = _uiState.value.playheadMs
        updateProject { proj ->
            proj.copy(
                audioClips = proj.audioClips + AudioClip(
                    title = name,
                    mediaUri = uri.toString(),
                    startMs = atMs,
                    durationMs = 8000L
                ),
                tracks = proj.tracks.copy(a1Muted = false)
            )
        }
        _uiState.update { it.copy(statusBanner = "Imported '$name' to Track A1") }
    }

    fun deleteAudioClip(audioId: String) {
        updateProject { proj ->
            proj.copy(audioClips = proj.audioClips.filterNot { it.id == audioId })
        }
    }

    fun transformCaption(captionId: String, zoomFactor: Float, panDxPx: Float, panDyPx: Float, rotDeg: Float) {
        updateProject(recordUndo = false) { proj ->
            proj.copy(
                captionClips = proj.captionClips.map { c ->
                    if (c.id == captionId) {
                        c.copy(
                            posX = (c.posX + panDxPx / 260f).coerceIn(0.08f, 0.92f),
                            posY = (c.posY + panDyPx / 360f).coerceIn(0.08f, 0.92f),
                            fontSizeSp = (c.fontSizeSp * zoomFactor).coerceIn(12f, 52f),
                            rotationDeg = (c.rotationDeg + rotDeg) % 360f
                        )
                    } else c
                }
            )
        }
    }

    fun moveCaptionPosition(captionId: String, dxNorm: Float, dyNorm: Float) {
        updateProject(recordUndo = false) { proj ->
            proj.copy(
                captionClips = proj.captionClips.map { c ->
                    if (c.id == captionId) {
                        c.copy(
                            posX = (c.posX + dxNorm).coerceIn(0.1f, 0.9f),
                            posY = (c.posY + dyNorm).coerceIn(0.1f, 0.92f)
                        )
                    } else c
                }
            )
        }
    }

    fun runAutoBeatCutAi() {
        val proj = _uiState.value.project
        val baseClips = proj.videoClips.filter { it.mediaUri.isNotBlank() }
        if (baseClips.isEmpty()) {
            _uiState.update { it.copy(statusBanner = "Import video/photo clips first for Auto Beat-Cut") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Detecting music beats & auto-cutting clips...") }
            val audioUri = proj.audioClips.firstOrNull { it.mediaUri.isNotBlank() }?.mediaUri ?: ""
            val totalDur = proj.totalDurationMs.coerceAtLeast(6000L)
            val beats = beatDetector.detectBeatTimestampsMs(audioUri, totalDur)
                .distinct()
                .sorted()
                .filter { it in 400L..(totalDur - 400L) }
                .take(12)

            val cutPoints = (listOf(0L) + beats + listOf(totalDur)).distinct().sorted()
            val transitions = listOf(TransitionType.FLASH_CUT, TransitionType.WHIP_PAN, TransitionType.ZOOM_THROUGH)
            val newClips = mutableListOf<VideoClip>()

            for (i in 0 until cutPoints.lastIndex) {
                val segDur = (cutPoints[i + 1] - cutPoints[i]).coerceAtLeast(450L)
                val src = baseClips[i % baseClips.size]
                val maxStart = (src.sourceDurationMs - segDur).coerceAtLeast(0L)
                val start = ((i * 700L) % (maxStart + 1L)).coerceAtLeast(0L)
                val end = (start + segDur).coerceAtMost(src.sourceDurationMs.coerceAtLeast(start + 450L))
                newClips.add(
                    src.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "Beat ${i + 1}",
                        trimStartMs = start,
                        trimEndMs = end,
                        videoFx = if (i % 2 == 0) VideoFxPreset.ZOOM_PULSE else VideoFxPreset.ACTION_FLASH,
                        transitionOut = if (i < cutPoints.lastIndex - 1) transitions[i % transitions.size] else TransitionType.NONE,
                        transitionDurationMs = 250L
                    )
                )
            }

            updateProject { p ->
                val updatedAudio = if (p.audioClips.isEmpty()) {
                    listOf(AudioClip(title = "Beat Sync Track", beatMarkersMs = beats))
                } else {
                    p.audioClips.mapIndexed { idx, a -> if (idx == 0) a.copy(beatMarkersMs = beats) else a }
                }
                p.copy(videoClips = newClips, audioClips = updatedAudio)
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    selectedVideoIndex = 0,
                    playheadMs = 0L,
                    statusBanner = "Auto Beat-Cut AI: Synced ${newClips.size} cuts to music beats!"
                )
            }
        }
    }

    fun detectBeatsOnAudio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Analyzing audio beats...") }
            val audioUri = _uiState.value.project.audioClips.firstOrNull()?.mediaUri ?: ""
            val beats = beatDetector.detectBeatTimestampsMs(audioUri, _uiState.value.project.totalDurationMs)
            updateProject { proj ->
                val clips = if (proj.audioClips.isEmpty()) {
                    listOf(AudioClip(title = "Beat Track", beatMarkersMs = beats))
                } else {
                    proj.audioClips.mapIndexed { idx, c -> if (idx == 0) c.copy(beatMarkersMs = beats) else c }
                }
                proj.copy(audioClips = clips)
            }
            _uiState.update { it.copy(isBusy = false, statusBanner = "Synced ${beats.size} beat markers on A1") }
        }
    }

    fun runAutoDubbing(
        targetLanguage: CaptionLanguage,
        muteOriginalVideo: Boolean,
        alsoAddCaptions: Boolean,
        customLanguageName: String = ""
    ) {
        if (!trySpendProCoins(5)) return
        val langTitle = customLanguageName.trim().ifBlank { targetLanguage.label }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Translating & Dubbing into $langTitle...") }
            val capRes = geminiEngine.generateAutoCaptions(
                project = _uiState.value.project,
                language = targetLanguage,
                style = CaptionStylePreset.CINEMA_SUB,
                colorHex = 0xFFF8FAFC,
                accentHex = 0xFFF59E0B,
                fontSizeSp = 20f,
                posY = 0.82f,
                customLanguageName = customLanguageName
            )
            val segments = capRes.getOrNull()
            if (segments.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusBanner = capRes.exceptionOrNull()?.message ?: "Could not extract speech for dubbing"
                    )
                }
                return@launch
            }
            val fullScript = segments.joinToString(". ") { it.text.trim() }
            val ttsTag = when (targetLanguage.code) {
                "hi", "bho", "bgc", "raj", "mwr", "awa", "hne", "gbm", "mag", "mai", "sa" -> "hi-IN"
                "en", "hi-Latn" -> "en-US"
                "pa" -> "pa-IN"
                "ta" -> "ta-IN"
                "te" -> "te-IN"
                "mr" -> "mr-IN"
                "bn" -> "bn-IN"
                "gu" -> "gu-IN"
                "kn" -> "kn-IN"
                "ml" -> "ml-IN"
                "ur" -> "ur-PK"
                "es" -> "es-ES"
                "fr" -> "fr-FR"
                "de" -> "de-DE"
                "ja" -> "ja-JP"
                "ko" -> "ko-KR"
                "zh" -> "zh-CN"
                "ar" -> "ar-SA"
                else -> targetLanguage.code
            }
            val ttsRes = ttsEngine.synthesizeVoiceover(fullScript, ttsTag)
            ttsRes.onSuccess { (uriStr, durMs) ->
                updateProject { proj ->
                    val updatedVideos = if (muteOriginalVideo) {
                        proj.videoClips.map { it.copy(muted = true) }
                    } else proj.videoClips
                    proj.copy(
                        videoClips = updatedVideos,
                        audioClips = proj.audioClips + AudioClip(
                            title = "Dub: $langTitle",
                            mediaUri = uriStr,
                            startMs = 0L,
                            durationMs = durMs,
                            isVoiceover = true,
                            voiceEnhanceEnabled = true
                        ),
                        captionClips = if (alsoAddCaptions) segments else proj.captionClips,
                        tracks = proj.tracks.copy(a1Muted = false, t1Visible = if (alsoAddCaptions) true else proj.tracks.t1Visible)
                    )
                }
                _uiState.update {
                    it.copy(isBusy = false, statusBanner = "Auto-Dubbed into $langTitle on Track A1")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, statusBanner = err.message ?: "Dubbing TTS failed")
                }
            }
        }
    }

    fun toggleVoiceSampleMatcher() {
        if (!_uiState.value.isRecordingVoiceSample) {
            val res = micRecorder.startRecording()
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isRecordingVoiceSample = true,
                        statusBanner = "Speak for 5 seconds... Tap Stop to Match Voice"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusBanner = err.message ?: "Mic permission required for Voice Matcher")
                }
            }
        } else {
            val res = micRecorder.stopRecording()
            res.onSuccess { (uriStr, _) ->
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isRecordingVoiceSample = false,
                            isBusy = true,
                            statusBanner = "Analyzing your voice Pitch, Speed & Language..."
                        )
                    }
                    val matchRes = geminiEngine.analyzeVoiceSample(uriStr)
                    matchRes.onSuccess { m ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                matchedPitch = m.pitch,
                                matchedSpeed = m.speed,
                                matchedLangTag = m.languageTag,
                                voiceProfileSummary = m.summary,
                                statusBanner = "Voice Profile Saved: ${m.summary}"
                            )
                        }
                    }.onFailure { err ->
                        _uiState.update {
                            it.copy(isBusy = false, statusBanner = err.message ?: "Voice match failed")
                        }
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isRecordingVoiceSample = false, statusBanner = err.message ?: "Sample failed")
                }
            }
        }
    }

    fun generateAiVoiceScript(topic: String, languageLabel: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Writing AI Voiceover Script...") }
            val res = geminiEngine.generateVoiceoverScript(topic, languageLabel)
            res.onSuccess { script ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        generatedVoiceScript = script,
                        statusBanner = "AI Script Ready!"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "Script error") }
            }
        }
    }

    fun setVoiceSampleUri(uri: Uri) {
        val label = queryDisplayName(uri) ?: "Voice Sample"
        _uiState.update {
            it.copy(
                voiceSampleUri = uri.toString(),
                voiceSampleLabel = label,
                statusBanner = "Loaded voice sample: $label"
            )
        }
    }

    fun cloneVoiceFromSampleAndSynthesize(
        script: String,
        voiceName: String,
        apiKey: String
    ) {
        val sampleUri = _uiState.value.voiceSampleUri
        if (sampleUri.isNullOrBlank()) {
            _uiState.update { it.copy(statusBanner = "Pick or record a voice sample first") }
            return
        }
        if (script.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Uploading sample & cloning voice via ElevenLabs...") }
            val res = ttsEngine.cloneAndSynthesizeElevenLabsVoice(sampleUri, voiceName, script, apiKey)
            res.onSuccess { (clonedId, uriStr, durMs) ->
                val atMs = _uiState.value.playheadMs
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = "Cloned Voice (${clonedId.take(6)})",
                            mediaUri = uriStr,
                            startMs = atMs,
                            durationMs = durMs,
                            isVoiceover = true,
                            voiceEnhanceEnabled = true
                        ),
                        tracks = proj.tracks.copy(a1Muted = false)
                    )
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusBanner = "Cloned & added audio to Track A1 (ID: $clonedId)"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "Voice cloning failed") }
            }
        }
    }

    fun synthesizeProVoiceover(
        script: String,
        languageTag: String,
        pitch: Float,
        speechRate: Float,
        elevenApiKey: String,
        elevenVoiceId: String
    ) {
        if (script.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Generating AI Voiceover...") }
            val res = if (elevenApiKey.isNotBlank() && elevenVoiceId.isNotBlank()) {
                ttsEngine.synthesizeElevenLabsVoice(script, elevenApiKey, elevenVoiceId)
            } else {
                ttsEngine.synthesizeVoiceover(script, languageTag, pitch, speechRate)
            }
            res.onSuccess { (uriStr, durMs) ->
                val atMs = _uiState.value.playheadMs
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = if (elevenApiKey.isNotBlank()) "AI Clone VO" else "AI Voice (${languageTag})",
                            mediaUri = uriStr,
                            startMs = atMs,
                            durationMs = durMs,
                            isVoiceover = true,
                            voiceEnhanceEnabled = true
                        ),
                        tracks = proj.tracks.copy(a1Muted = false)
                    )
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusBanner = "Added AI Voiceover to Track A1 at ${atMs}ms"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "Voiceover failed") }
            }
        }
    }

    fun toggleMicRecording() {
        if (!_uiState.value.isRecordingMic) {
            val res = micRecorder.startRecording()
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        isRecordingMic = true,
                        statusBanner = "Recording Mic Voiceover... Tap Stop when done"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusBanner = err.message ?: "Mic permission required to record")
                }
            }
        } else {
            val res = micRecorder.stopRecording()
            res.onSuccess { (uriStr, durMs) ->
                val atMs = _uiState.value.playheadMs
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = "Mic Voiceover",
                            mediaUri = uriStr,
                            startMs = atMs,
                            durationMs = durMs,
                            isVoiceover = true,
                            voiceEnhanceEnabled = true
                        ),
                        tracks = proj.tracks.copy(a1Muted = false)
                    )
                }
                _uiState.update {
                    it.copy(
                        isRecordingMic = false,
                        statusBanner = "Saved Mic Voiceover (${durMs / 1000}s) on Track A1"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isRecordingMic = false, statusBanner = err.message ?: "Recording save failed")
                }
            }
        }
    }

    fun generateVoiceover(script: String, languageTag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Synthesizing AI voiceover...") }
            val res = ttsEngine.synthesizeVoiceover(script, languageTag)
            res.onSuccess { (uriStr, durMs) ->
                updateProject { proj ->
                    proj.copy(
                        audioClips = proj.audioClips + AudioClip(
                            title = "VO ($languageTag)",
                            mediaUri = uriStr,
                            startMs = _uiState.value.playheadMs,
                            durationMs = durMs,
                            isVoiceover = true
                        )
                    )
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Added AI Voiceover to A1") }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "TTS error") }
            }
        }
    }

    fun startTapMotionTracking(mode: String) {
        _uiState.update {
            it.copy(
                activeSheet = ActiveStudioSheet.NONE,
                isMotionTrackerPicking = true,
                pendingTrackMode = mode,
                statusBanner = "Tap subject on video/photo to lock $mode track"
            )
        }
    }

    fun startMaskScreenEdit() {
        _uiState.update {
            it.copy(
                activeSheet = ActiveStudioSheet.NONE,
                isMaskEditMode = true,
                statusBanner = "Drag, pinch & rotate Mask on screen"
            )
        }
    }

    fun stopMaskScreenEdit() {
        _uiState.update { it.copy(isMaskEditMode = false, statusBanner = null) }
    }

    fun transformSelectedMask(zoomFactor: Float, panDx: Float, panDy: Float, rotateDelta: Float) {
        updateSelectedClip { c ->
            val nextScale = (c.maskScale * zoomFactor).coerceIn(0.25f, 1.4f)
            val nextX = (c.maskOffsetX + panDx / 500f).coerceIn(-0.45f, 0.45f)
            val nextY = (c.maskOffsetY + panDy / 700f).coerceIn(-0.45f, 0.45f)
            val nextRot = (((c.maskRotationDeg + rotateDelta) % 360f) + 360f) % 360f
            c.copy(
                maskScale = nextScale,
                maskOffsetX = nextX,
                maskOffsetY = nextY,
                maskRotationDeg = nextRot
            )
        }
    }

    fun cancelTapMotionTracking() {
        _uiState.update { it.copy(isMotionTrackerPicking = false, statusBanner = null) }
    }

    fun setMotionTrackMode(mode: String) {
        _uiState.update { it.copy(pendingTrackMode = mode) }
        updateSelectedClip { it.copy(motionTrackMode = mode) }
    }

    fun clearMotionTracking() {
        _uiState.update { it.copy(isMotionTrackerPicking = false) }
        updateSelectedClip { it.copy(motionTrackPoints = emptyList()) }
        _uiState.update { it.copy(statusBanner = "Motion tracking cleared") }
    }

    fun runMotionTracking(targetNormX: Float, targetNormY: Float, mode: String = _uiState.value.pendingTrackMode) {
        val targetIdx = playheadClipIndexAndNormT().first
        val clip = _uiState.value.project.videoClips.getOrNull(targetIdx) ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    isMotionTrackerPicking = false,
                    pendingTrackMode = mode,
                    statusBanner = "Tracking $mode motion across frames..."
                )
            }
            val points = visionEngine.trackMotionAcrossClip(
                mediaUri = clip.mediaUri,
                startMs = clip.trimStartMs,
                endMs = clip.trimEndMs,
                targetNormX = targetNormX,
                targetNormY = targetNormY,
                mode = mode
            )
            updateSelectedClip { it.copy(motionTrackPoints = points, motionTrackMode = mode) }
            _uiState.update { it.copy(isBusy = false, statusBanner = "$mode Track locked (${points.size} keyframes)") }
        }
    }

    fun runSmartAutoReframe() {
        val targetIdx = playheadClipIndexAndNormT().first
        val clip = _uiState.value.project.videoClips.getOrNull(targetIdx) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Computing Smart Auto-Reframe...") }
            val offset = visionEngine.computeSmartReframeOffset(clip.mediaUri, clip.trimStartMs + 500L)
            updateSelectedClip { it.copy(reframeOffsetX = offset) }
            _uiState.update { it.copy(isBusy = false, statusBanner = "Smart Auto-Reframe applied (${(offset * 100).toInt()}%)") }
        }
    }

    fun openLoginDialog(show: Boolean) {
        _uiState.update { it.copy(showLoginDialog = show) }
    }

    fun loginProAccount(name: String, email: String) {
        val nextAcc = accountManager.login(name, email)
        _uiState.update {
            it.copy(
                account = nextAcc,
                showLoginDialog = false,
                statusBanner = "Welcome ${nextAcc.userName}! ${nextAcc.dailyCoins} Pro Coins credited today."
            )
        }
    }

    fun logoutProAccount() {
        val nextAcc = accountManager.logout()
        _uiState.update {
            it.copy(
                account = nextAcc,
                showLoginDialog = false,
                statusBanner = "Signed out of Pro Account"
            )
        }
    }

    private fun trySpendProCoins(cost: Int = 5): Boolean {
        val (ok, updatedAcc) = accountManager.spendCoins(cost)
        _uiState.update { it.copy(account = updatedAcc) }
        if (!ok) {
            _uiState.update {
                it.copy(
                    activeSheet = if (!updatedAcc.isLoggedIn) ActiveStudioSheet.NONE else it.activeSheet,
                    showLoginDialog = !updatedAcc.isLoggedIn,
                    statusBanner = if (!updatedAcc.isLoggedIn) {
                        "Login to claim 50 daily Pro Coins!"
                    } else {
                        "Daily 50 Coins used up — resets tomorrow!"
                    }
                )
            }
        }
        return ok
    }

    fun openFlowAiInBrowser(prompt: String) {
        if (!trySpendProCoins(5)) return
        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Flow AI Prompt", prompt))
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://labs.google/fx/tools/flow")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(intent) }
        _uiState.update {
            it.copy(statusBanner = "Prompt copied! Opening Google Flow AI (-5 Coins)")
        }
    }

    fun runAiAutoCreator(prompt: String) {
        if (_uiState.value.project.videoClips.isEmpty()) {
            _uiState.update { it.copy(activeSheet = ActiveStudioSheet.NONE, statusBanner = "Import a video or photo first to run AI Auto-Edit") }
            return
        }
        if (!trySpendProCoins(5)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Gemini 2.5 Flash analyzing sequence...") }
            val summary = geminiEngine.runAutoCreator(prompt, _uiState.value.project)
            updateProject { summary.updatedProject }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeSheet = ActiveStudioSheet.NONE,
                    lastCorrectionReport = summary.message,
                    statusBanner = "${summary.message} (-5 Coins)"
                )
            }
        }
    }

    fun enhancePhotoUri(uri: Uri? = null, sharpness: Float = 0.5f) {
        val idx = playheadClipIndexAndNormT().first
        val currentClip = _uiState.value.project.videoClips.getOrNull(idx)
        val targetUri = uri?.toString() ?: currentClip?.mediaUri
        if (targetUri.isNullOrBlank()) {
            _uiState.update { it.copy(statusBanner = "Pick a photo to enhance") }
            return
        }
        if (uri == null && currentClip != null) {
            val mime = runCatching { getApplication<Application>().contentResolver.getType(Uri.parse(targetUri)) }.getOrNull().orEmpty()
            val isImg = currentClip.isAiGeneratedBroll || mime.startsWith("image/") ||
                targetUri.endsWith(".jpg", true) || targetUri.endsWith(".jpeg", true) ||
                targetUri.endsWith(".png", true) || targetUri.endsWith(".webp", true)
            if (!isImg) {
                enhanceVideoPreset("4K_ULTRA")
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Enhancing Photo Clarity & Upscaling...") }
            val res = photoEnhancer.enhancePhotoToFile(targetUri, sharpness = sharpness)
            res.onSuccess { enhancedUri ->
                if (uri != null) {
                    val clip = VideoClip(
                        title = "Enhanced Photo HD",
                        mediaUri = enhancedUri,
                        isAiGeneratedBroll = true,
                        sourceDurationMs = 3500L,
                        trimStartMs = 0L,
                        trimEndMs = 3500L
                    )
                    updateProject { proj ->
                        proj.copy(videoClips = proj.videoClips + clip)
                    }
                } else {
                    updateSelectedClip { it.copy(mediaUri = enhancedUri, isAiGeneratedBroll = true, title = "Enhanced HD") }
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        activeSheet = ActiveStudioSheet.NONE,
                        statusBanner = "Photo Enhanced (HD Upscale + Unsharp Clarity)",
                        lastCorrectionReport = "Photo Enhanced: 1.5x HD Upscale + Unsharp Clarity + Contrast Boost"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "Photo enhance failed") }
            }
        }
    }

    fun enhanceVideoPreset(level: String) {
        updateSelectedClip { clip ->
            when (level) {
                "4K_ULTRA" -> clip.copy(
                    colorGrade = clip.colorGrade.copy(
                        contrast = 1.22f,
                        saturation = 1.16f,
                        shadows = 0.14f,
                        highlights = -0.10f,
                        beautyGlow = 0.18f,
                        lutName = "4K Ultra Crisp"
                    )
                )
                "SKIN_RETOUCH" -> clip.copy(
                    colorGrade = clip.colorGrade.copy(
                        contrast = 1.10f,
                        saturation = 1.08f,
                        warmth = 0.08f,
                        beautySmooth = 0.45f,
                        beautyGlow = 0.35f,
                        lutName = "Skin Retouch Pro"
                    )
                )
                else -> clip.copy(
                    colorGrade = clip.colorGrade.copy(
                        contrast = 1.16f,
                        saturation = 1.12f,
                        shadows = 0.10f,
                        highlights = -0.06f,
                        lutName = "HD Crisp"
                    )
                )
            }
        }
        if (level == "4K_ULTRA") {
            updateProject(recordUndo = false) { p ->
                p.copy(exportSettings = p.exportSettings.copy(resolution = ExportResolution.UHD_4K))
            }
        }
        _uiState.update {
            it.copy(
                activeSheet = ActiveStudioSheet.NONE,
                statusBanner = "Video Enhancer Applied: $level",
                lastCorrectionReport = "Video Enhanced ($level): Dynamic Range + Clarity + Crispness Boost"
            )
        }
    }

    fun enhanceVoiceOneTap(enable: Boolean) {
        updateAudioDsp(
            windFilter = enable,
            noiseGate = enable,
            voiceEnhance = enable,
            vocalMode = if (enable) VocalIsolationMode.ISOLATE_VOCALS else VocalIsolationMode.FULL_MIX
        )
        _uiState.update {
            it.copy(
                statusBanner = if (enable) "Voice Enhancer ON (Studio Mic + Noise Gate)" else "Voice Enhancer OFF",
                lastCorrectionReport = if (enable) "Voice Enhanced: Noise Gate + Wind Filter + Podcast Clarity EQ" else null
            )
        }
    }

    fun runAutoColorCorrection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Analyzing exposure & white balance...") }
            val summary = geminiEngine.runAutoColorCorrection(_uiState.value.project, playheadClipIndexAndNormT().first)
            updateProject { summary.updatedProject }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    statusBanner = summary.message,
                    lastCorrectionReport = summary.message
                )
            }
        }
    }

    fun runAiColorGrade(stylePrompt: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Grading with Gemini LUT...") }
            val summary = geminiEngine.runAiColorGrade(stylePrompt, _uiState.value.project, playheadClipIndexAndNormT().first)
            updateProject { summary.updatedProject }
            _uiState.update { it.copy(isBusy = false, statusBanner = summary.message, lastCorrectionReport = summary.message) }
        }
    }

    fun runAiCopilot(command: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "AI Copilot executing...") }
            val summary = geminiEngine.runAiCopilotCommand(command, _uiState.value.project, playheadClipIndexAndNormT().first)
            updateProject { summary.updatedProject }
            _uiState.update { it.copy(isBusy = false, statusBanner = summary.message, lastCorrectionReport = summary.message) }
        }
    }

    fun generateAiBroll(prompt: String) {
        if (!trySpendProCoins(5)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusBanner = "Generating AI B-Roll...") }
            val res = geminiEngine.generateAiBrollImage(prompt)
            res.onSuccess { combined ->
                val uriStr = combined.substringBefore("|")
                updateProject { proj ->
                    proj.copy(
                        videoClips = proj.videoClips + VideoClip(
                            title = "AI: ${prompt.take(14)}",
                            mediaUri = uriStr,
                            isAiGeneratedBroll = true,
                            sourceDurationMs = 4000L,
                            trimEndMs = 4000L
                        )
                    )
                }
                _uiState.update { it.copy(isBusy = false, statusBanner = "Inserted AI B-Roll onto V1") }
            }.onFailure { err ->
                _uiState.update { it.copy(isBusy = false, statusBanner = err.message ?: "B-Roll failed") }
            }
        }
    }

    fun updateExportSettings(settings: ExportSettings) {
        updateProject(recordUndo = false) { it.copy(exportSettings = settings) }
    }

    fun cancelExport() {
        activeExportJob?.cancel()
        activeExportJob = null
        _uiState.update { it.copy(isExporting = false, exportProgress = null, isBusy = false, statusBanner = "Export cancelled") }
    }

    fun generateProxyForSelectedClip() {
        val idx = playheadClipIndexAndNormT().first
        val clip = _uiState.value.project.videoClips.getOrNull(idx) ?: return
        if (clip.mediaUri.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(statusBanner = "Generating 540p smooth proxy...") }
            proxyEngine.generate540pProxy(clip.mediaUri).onSuccess { pUri ->
                updateProject(recordUndo = false) { proj ->
                    val list = proj.videoClips.toMutableList()
                    if (idx in list.indices) list[idx] = list[idx].copy(proxyUri = pUri)
                    proj.copy(videoClips = list)
                }
                _uiState.update { it.copy(statusBanner = "540p Proxy active for smooth preview") }
            }.onFailure { err ->
                _uiState.update { it.copy(statusBanner = err.message ?: "Proxy skipped") }
            }
        }
    }

    fun startExport() {
        activeExportJob?.cancel()
        activeExportJob = viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, isExporting = true, exportProgress = 0.05f, statusBanner = "Exporting zero-watermark master...") }
            val res = exportEngine.exportProject(_uiState.value.project) { progress ->
                _uiState.update { it.copy(exportProgress = progress) }
            }
            res.onSuccess { path ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isExporting = false,
                        exportProgress = null,
                        lastExportPath = path,
                        statusBanner = "Saved to $path"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isExporting = false,
                        exportProgress = null,
                        statusBanner = err.message ?: "Export failed"
                    )
                }
            }
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            getApplication<Application>().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun queryMediaDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<Application>(), uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 4000L
        } catch (_: Exception) {
            4000L
        } finally {
            runCatching { retriever.release() }
        }
    }


    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        micRecorder.stopSilently()
    }

}
