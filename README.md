# KB Editor — Flagship Android Video Editing Suite

**Package:** `com.aistudio.kbeditor.r7v4qx`  
**Min SDK:** 26 (Android 8.0) | **Target / Compile SDK:** 35 (Android 15)

## Setup in Android Studio
1. Open the `KBEditor` folder in **Android Studio Ladybug (2024.2+)** or newer.
2. Add your Firebase project's `google-services.json` into `app/google-services.json` to enable live **Firebase AI Logic** (`gemini-3.8-flash` & `gemini-nano-banana-2.1`).
3. Sync Gradle and run on a physical Android device or emulator (API 26+).

## Included Modules
- `ui/` — Adaptive handheld & tablet studio (`KbEditorScreen`), `VerticalPreviewStage`, `MultiTrackTimeline`, `ProResolveVfxPanels` (3-way color wheels & 2D tracker), `ProColorVfxSheets`, `ProStudioHubs`, `InspectorSheets`.
- `vfx/` — `CurveEvaluators` for optical flow speed ramps, keyframe interpolation, and 3-way `ColorMatrix`.
- `vision/` — `SmartVisionEngine` (`FaceDetector` + 2D SAD optical block-matching & Smart Auto-Reframe).
- `audio/` — `AudioExtractorEngine`, `StudioAudioDspProcessor`, `AudioBeatDetector`, `VoiceoverTtsEngine`.
- `media/` — `VideoExportEngine` (Media3 `Transformer` 720p/1080p/4K UHD, 24/30/60fps, SDR H.264 & HDR10 HLG HEVC).
- `ai/` — `GeminiStudioEngine` (AI Auto-Creator, AI Color Grade, Copilot, Generative B-Roll).
- `storage/` — `ProjectStorage` (30-step Undo/Redo + `kb_editor_project.json` persistence).
