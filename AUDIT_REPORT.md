# KBEditor v6 — static audit and patch report

## Patched in this archive

1. **V1 source audio was incorrectly tied to the A1 mute switch.**
   - `app/src/main/java/com/aistudio/kbeditor/r7v4qx/ui/VerticalPreviewStage.kt:352-358`
   - `app/src/main/java/com/aistudio/kbeditor/r7v4qx/media/VideoExportEngine.kt:284-293`
   - A1 mute now affects A1 audio clips only. V1 source audio uses `VideoClip.muted` and `VideoClip.volume` independently.

2. **A1 audio clips were serialized in one sequence.** Overlapping music/voiceover/SFX therefore played one after another and later clips lost their intended `startMs` positions. `AudioClip.volume` and `durationMs` were also not applied during export.
   - `VideoExportEngine.kt:299-331`
   - Each audio clip now has its own parallel composition sequence with its own timeline gap, duration clipping, and DSP output gain.

3. **The H.264 fallback export dropped all A1 audio sequences.**
   - `VideoExportEngine.kt:359-365`
   - Fallback now uses the full `sequences` list instead of only the video sequence.

4. **Mono instrumental-only mode did not apply.** The mono branch ignored `vocalMode` and passed the original mono signal through.
   - `audio/StudioAudioDspProcessor.kt:352-355`
   - `cpp/kbeditor_native.cpp:305-307`
   - Mono has no side channel; instrumental-only now outputs silence. Vocal/mid mode remains the original mono signal.

5. **Native bitmap enhancement ignored Android row padding.** It assumed each row was exactly `width * 4` bytes, which can address the wrong pixels on bitmaps whose `stride` is larger.
   - `cpp/kbeditor_native.cpp:325-354`
   - Pixel access now uses `info.stride` per row.

## Confirmed remaining functional gaps (not silently represented as fixed)

1. **Export FPS selector is not connected to the encoder.** `EditorModels.kt:457-460` exposes 24/30/60 fps, but `VideoExportEngine.kt:381-400` builds the Transformer without applying `project.exportSettings.fps`. The output frame rate is therefore not guaranteed to match the selected value. Add a frame-rate conversion effect/encoder path and verify the result with MediaInfo on device.

2. **Keyframes, motion-tracking points, and Magic Eraser are preview-only in the export path.** Preview consumes keyframes/motion points in `ui/VerticalPreviewStage.kt:113-114`; the live GL preview sends eraser points in `vfx/KbLiveGlPreviewView.kt:131-138`. `VideoExportEngine.kt` does not consume `clip.keyframes`, `clip.motionTrackPoints`, or `clip.magicEraserPoints`. Add matching time-dependent export GL effects before claiming these edits survive export.

3. **Preview A1 audio does not mix multiple overlapping music/voiceover clips.** `VerticalPreviewStage.kt:361-365` selects only the last active non-SFX clip and owns one `audioPlayer`; export now mixes clips as parallel sequences. Use a player per active clip or a proper multi-track preview mixer to make preview match export.

4. **Face Swap is a feathered face-patch overlay, not a full identity/landmark face-swap pipeline.** `vision/FaceSwapEngine.kt:49-59, 70-116` detects the largest face, center-crops it, scales it to 256×256 and applies an oval feather. There is no landmark alignment, pose warping, expression transfer, or generative identity synthesis in this engine.

5. **Voice Clone endpoint is TTS using a caller-supplied ElevenLabs voice ID, not an in-app voice-cloning workflow.** `audio/VoiceoverTtsEngine.kt:109-145` calls `/v1/text-to-speech/{voiceId}`. A voice must already exist in the user's ElevenLabs account; the app does not create/train a clone.

6. **The “10k+ effects/animations” catalog count is mostly generated variations.** `model/EffectCatalog.kt:156-183, 235-263` creates renamed speed/intensity/style variants mapped to existing presets. These are selectable variants, not 10,000 independent shader implementations.

## Build verification status

- Static source review completed for the supplied Kotlin/C++/Gradle project files.
- A full Gradle/Android build was **not run** in this environment: `gradle` is not installed and the ZIP does not include `gradlew`/wrapper files. Consequently, this report does not claim a successful compile or GitHub Actions build.
- No definitive Kotlin syntax/import/type error was established by static review. The patched Android/Kotlin and JNI changes still require `assembleDebug` on an Android SDK + NDK-equipped runner.
