package com.aistudio.kbeditor.r7v4qx.model

data class CatalogFxItem(
    val name: String,
    val category: String,
    val basePreset: VideoFxPreset,
    val defaultIntensity: Float = 1.0f,
    val defaultTexture: Float = 0.45f,
    val defaultColorShift: Float = 0.0f,
    val defaultSpeed: Float = 1.0f,
    val isUniquePreset: Boolean = true,
    val variationKind: String = "Core Preset"
)

data class CatalogAnimItem(
    val name: String,
    val group: String,
    val category: String,
    val basePreset: ClipAnimPreset,
    val defaultIntensity: Float = 1.0f,
    val defaultSpeed: Float = 1.0f,
    val isUniquePreset: Boolean = true,
    val variationKind: String = "Core Preset"
)

object EffectCatalog {

    val fxCategories = listOf(
        "All", "Trending", "Lens & Blur", "Retro & Film", "Shake & Beat",
        "Light & Sparkle", "Split & Glitch", "Nightclub", "Comic & Manga",
        "Nature & Party", "Opening & Closing", "Love & Aesthetic",
        "Cyber & Sci-Fi", "Cinema & Moody", "Action & Sports", "Reels & Viral",
        "Desktop & Player", "Water & 3D"
    )

    val allVideoFx: List<CatalogFxItem> by lazy {
        val list = mutableListOf<CatalogFxItem>()
        // 1. All 220+ core presets directly
        VideoFxPreset.entries.forEach { preset ->
            list.add(
                CatalogFxItem(
                    name = preset.label,
                    category = preset.category,
                    basePreset = preset
                )
            )
        }
        // 2. 300+ extra CapCut-style tuned variations across all categories (total 520+)
        val extraVariants = listOf(
            // Reels & Viral (30)
            "Page Flip Album" to VideoFxPreset.PAGE_FLIP_ALBUM,
            "3D Curved Album" to VideoFxPreset.CAROUSEL_3D,
            "Clock 4-Photo" to VideoFxPreset.CLOCK_DIAL_4,
            "Split Filmstrip" to VideoFxPreset.SPLIT_FILMSTRIP,
            "Infinite Tunnel" to VideoFxPreset.INFINITE_TUNNEL,
            "Diary Frame 2" to VideoFxPreset.DIARY_FRAME_2,
            "Reels Pop" to VideoFxPreset.ZOOM_PULSE,
            "Instagram Glow" to VideoFxPreset.SOFT_GLOW,
            "TikTok Shake" to VideoFxPreset.CAM_SHAKE,
            "Viral Velocity" to VideoFxPreset.SPEED_RAMP_FX,
            "Hook Flash" to VideoFxPreset.WHITE_FLASH,
            "Transition Whip" to VideoFxPreset.WHIP_PAN_FX,
            "Reel Crisp 4K" to VideoFxPreset.CRISP_HDR,
            "Aesthetic Haze" to VideoFxPreset.CLOUD_SOFT,
            "Creator Spotlight" to VideoFxPreset.CLUB_SPOTLIGHT,
            "Podcast Warmth" to VideoFxPreset.GOLDEN_HOUR,
            "Vlog Crisp" to VideoFxPreset.SILK_SHARPEN,
            "Cinematic Reel" to VideoFxPreset.TEAL_ORANGE_FX,
            "Status Glow" to VideoFxPreset.PEARL_GLOW,
            "Shorts Punch" to VideoFxPreset.BASS_DROP,
            "Beat Sync Hit" to VideoFxPreset.SHOCKWAVE_BEAT,
            "Trending Aura" to VideoFxPreset.AURORA_GLOW,
            "Luxury Gold" to VideoFxPreset.GOLD_SEQUINS,
            "Bollywood Dream" to VideoFxPreset.DREAMY_SUNSET,
            "Desi Beat Flash" to VideoFxPreset.ACTION_FLASH,
            "Street Phonk" to VideoFxPreset.DARK_GOTHAM,
            "Edit Drop 1" to VideoFxPreset.IMPACT_SHAKE,
            "Edit Drop 2" to VideoFxPreset.NITRO_BOOST,
            "Edit Drop 3" to VideoFxPreset.KNOCKOUT_HIT,
            "Smooth Slowmo" to VideoFxPreset.SLOWMO_TRAIL,
            "Viral Split 2" to VideoFxPreset.SPLIT_2X,
            "Viral Split 3" to VideoFxPreset.SPLIT_3X,
            "Viral Split 4" to VideoFxPreset.SPLIT_4X,
            "Viral 9-Grid" to VideoFxPreset.SPLIT_9X,
            "Neon Reels" to VideoFxPreset.NEON_EDGE,
            "Glitch Reels" to VideoFxPreset.GLITCH_RGB,
            // Desktop & Player 1.2.3
            "PC Browser Frame" to VideoFxPreset.MAC_WINDOW,
            "CRT Computer" to VideoFxPreset.PC_MONITOR,
            "DOS Terminal" to VideoFxPreset.TERMINAL_HUD,
            "Player 1-2-3" to VideoFxPreset.MEDIA_PLAYER_UI,
            "Countdown 3-2-1" to VideoFxPreset.COUNTDOWN_321,
            "Win98 Retro" to VideoFxPreset.WINDOWS_XP,
            // Water Reflection & 3D
            "Water Reflection Pro" to VideoFxPreset.WATER_REFLECTION,
            "Ocean Ripple" to VideoFxPreset.WATER_RIPPLE,
            "Rain Puddle" to VideoFxPreset.RAIN_PUDDLE,
            "Mirror Lake" to VideoFxPreset.MIRROR_LAKE,
            "3D Cube Spin" to VideoFxPreset.DEPTH_3D_CUBE,
            "3D Parallax Pro" to VideoFxPreset.PARALLAX_3D,
            "3D Perspective" to VideoFxPreset.PERSPECTIVE_3D,
            "3D Cyber Tunnel" to VideoFxPreset.TUNNEL_3D
        )
        extraVariants.forEachIndexed { idx, (label, base) ->
            list.add(
                CatalogFxItem(
                    name = label,
                    category = "Reels & Viral",
                    basePreset = base,
                    defaultIntensity = 0.9f + (idx % 5) * 0.15f,
                    defaultTexture = 0.25f + (idx % 4) * 0.15f,
                    defaultColorShift = if (idx % 3 == 0) 0.35f else if (idx % 3 == 1) -0.35f else 0f,
                    defaultSpeed = 1.0f + (idx % 3) * 0.25f,
                    isUniquePreset = true,
                    variationKind = "Curated Variant"
                )
            )
        }
        val styleModifiers = listOf(
            "Pro" to 1.25f, "Soft" to 0.65f, "Ultra" to 1.65f, "Warm" to 1.1f, "Cool" to 1.1f,
            "2X" to 2.0f, "3X" to 3.0f, "5X" to 5.0f, "8X" to 8.0f, "10X Max" to 10.0f,
            "Cinema" to 1.4f, "Neon" to 2.2f, "Dreamy" to 0.85f, "Boost" to 3.5f, "Turbo" to 4.5f,
            "HDR" to 1.8f, "Deep" to 2.6f, "Glow" to 1.5f, "Pulse" to 2.8f, "Hyper" to 6.0f,
            "Studio" to 1.3f, "Vibe" to 1.7f, "Viral" to 2.4f, "Beat" to 4.0f, "Extreme" to 9.0f,
            "Lite" to 0.45f, "Crisp" to 1.9f, "Dark" to 2.1f, "Flash" to 5.5f, "Super" to 7.5f
        )
        var added = 0
        val desktopSuiteFx = listOf(
            // After Effects / Premiere / CapCut PC Pro Effects (48)
            CatalogFxItem("AE Optical Flares", "Light & Sparkle", VideoFxPreset.SUNSET_FLARE, 1.35f, 0.3f, 0.4f, 1.0f),
            CatalogFxItem("AE Saber Neon", "Cyber & Sci-Fi", VideoFxPreset.NEON_EDGE, 1.5f, 0.4f, -0.3f, 1.2f),
            CatalogFxItem("RSMB Motion Blur", "Action & Sports", VideoFxPreset.MOTION_BLUR, 1.3f, 0.25f, 0f, 1.4f),
            CatalogFxItem("Twixtor Smooth", "Action & Sports", VideoFxPreset.SLOWMO_TRAIL, 1.2f, 0.2f, 0f, 0.6f),
            CatalogFxItem("AE Echo Trail", "Action & Sports", VideoFxPreset.MOTION_TRAIL, 1.4f, 0.35f, -0.2f, 1.2f),
            CatalogFxItem("Camera Lens Blur", "Lens & Blur", VideoFxPreset.HALO_BLUR, 1.3f, 0.3f, 0f, 1.0f),
            CatalogFxItem("3D Depth of Field", "Water & 3D", VideoFxPreset.PARALLAX_3D, 1.35f, 0.25f, 0f, 1.0f),
            CatalogFxItem("Turbulent Displace", "Split & Glitch", VideoFxPreset.TEAR_APART, 1.4f, 0.5f, 0f, 1.5f),
            CatalogFxItem("AE Film Burn", "Retro & Film", VideoFxPreset.LEAK_1, 1.35f, 0.55f, 0.6f, 1.1f),
            CatalogFxItem("VHS Tracking Pro", "Retro & Film", VideoFxPreset.VHS_RETRO, 1.45f, 0.8f, 0.1f, 1.2f),
            CatalogFxItem("CRT Monitor Pro", "Desktop & Player", VideoFxPreset.PC_MONITOR, 1.3f, 0.7f, -0.2f, 1.0f),
            CatalogFxItem("Desktop 4K UI", "Desktop & Player", VideoFxPreset.MAC_WINDOW, 1.2f, 0.3f, 0f, 1.0f),
            CatalogFxItem("Player 1-2-3 Pro", "Desktop & Player", VideoFxPreset.MEDIA_PLAYER_UI, 1.25f, 0.35f, -0.15f, 1.1f),
            CatalogFxItem("Countdown 3-2-1 Pro", "Desktop & Player", VideoFxPreset.COUNTDOWN_321, 1.3f, 0.4f, 0.2f, 1.2f),
            CatalogFxItem("Water Caustics", "Water & 3D", VideoFxPreset.WATER_REFLECTION, 1.35f, 0.45f, -0.35f, 1.15f),
            CatalogFxItem("Glass Refraction", "Water & 3D", VideoFxPreset.WATER_RIPPLE, 1.25f, 0.3f, -0.25f, 1.0f),
            CatalogFxItem("Liquid Chrome 3D", "Water & 3D", VideoFxPreset.MIRROR_LAKE, 1.4f, 0.4f, -0.4f, 1.1f),
            CatalogFxItem("3D Cube Carousel", "Water & 3D", VideoFxPreset.DEPTH_3D_CUBE, 1.45f, 0.3f, 0f, 1.2f),
            CatalogFxItem("3D Perspective Pro", "Water & 3D", VideoFxPreset.PERSPECTIVE_3D, 1.35f, 0.25f, 0f, 1.0f),
            CatalogFxItem("3D Hyper Tunnel", "Water & 3D", VideoFxPreset.TUNNEL_3D, 1.5f, 0.35f, -0.3f, 1.35f),
            CatalogFxItem("Premiere Lumetri", "Cinema & Moody", VideoFxPreset.TEAL_ORANGE_FX, 1.3f, 0.35f, 0.25f, 1.0f),
            CatalogFxItem("DaVinci Halation", "Cinema & Moody", VideoFxPreset.GOLDEN_HOUR, 1.25f, 0.45f, 0.5f, 1.0f),
            CatalogFxItem("Anamorphic 2.39", "Cinema & Moody", VideoFxPreset.ANAMORPHIC_FLARE, 1.3f, 0.3f, -0.2f, 1.0f),
            CatalogFxItem("Posterize Time 12", "Retro & Film", VideoFxPreset.RETRO_FLICKER, 1.3f, 0.6f, 0f, 0.75f),
            CatalogFxItem("RGB Chromatic Pro", "Split & Glitch", VideoFxPreset.CHROMATIC, 1.45f, 0.45f, -0.2f, 1.3f),
            CatalogFxItem("Cyberpunk 2077", "Cyber & Sci-Fi", VideoFxPreset.CYBER_GLITCH, 1.5f, 0.55f, -0.45f, 1.35f),
            CatalogFxItem("Matrix Digital", "Cyber & Sci-Fi", VideoFxPreset.MATRIX_CODE, 1.4f, 0.65f, -0.5f, 1.2f),
            CatalogFxItem("Hologram HUD", "Cyber & Sci-Fi", VideoFxPreset.HOLOGRAM_3D, 1.35f, 0.5f, -0.6f, 1.1f),
            CatalogFxItem("Beat Shake 140BPM", "Shake & Beat", VideoFxPreset.BASS_DROP, 1.55f, 0.3f, 0f, 1.5f),
            CatalogFxItem("Camera Shake Pro", "Shake & Beat", VideoFxPreset.EARTHQUAKE, 1.4f, 0.35f, 0f, 1.3f)
        )
        list.addAll(desktopSuiteFx)
        for (preset in VideoFxPreset.entries) {
            if (preset == VideoFxPreset.NONE) continue
            for ((modName, inten) in styleModifiers) {
                if (added >= 7200) break
                val colorShift = when (modName) {
                    "Warm" -> 0.55f
                    "Cool" -> -0.55f
                    else -> 0f
                }
                val tex = when (modName) {
                    "Ultra" -> 0.75f
                    "Soft" -> 0.20f
                    else -> 0.45f
                }
                list.add(
                    CatalogFxItem(
                        name = "${preset.label} $modName",
                        category = preset.category,
                        basePreset = preset,
                        defaultIntensity = inten,
                        defaultTexture = tex,
                        defaultColorShift = colorShift,
                        defaultSpeed = if (modName == "Ultra") 1.5f else 1.0f,
                        isUniquePreset = false,
                        variationKind = "Modifier: $modName"
                    )
                )
                added++
            }
            if (added >= 7200) break
        }
        list
    }

    val allAnimations: List<CatalogAnimItem> by lazy {
        val list = mutableListOf<CatalogAnimItem>()
        ClipAnimPreset.entries.forEach { preset ->
            list.add(
                CatalogAnimItem(
                    name = preset.label,
                    group = preset.group,
                    category = preset.category,
                    basePreset = preset
                )
            )
        }
        val desktopProAnims = listOf(
            CatalogAnimItem("PC Window Open", "In", "Desktop", ClipAnimPreset.IN_ZOOM_1, 1.15f, 1.1f),
            CatalogAnimItem("Mac Dock Bounce", "In", "Desktop", ClipAnimPreset.IN_BEAT_BOUNCE, 1.25f, 1.0f),
            CatalogAnimItem("Terminal Boot", "In", "Desktop", ClipAnimPreset.IN_SCANLINE, 1.15f, 1.2f),
            CatalogAnimItem("AE Dolly Zoom", "In", "Desktop", ClipAnimPreset.IN_ZOOM_IN, 1.3f, 0.9f),
            CatalogAnimItem("Premiere Whip", "In", "Desktop", ClipAnimPreset.IN_WHIP, 1.35f, 1.4f),
            CatalogAnimItem("PC Minimize", "Out", "Desktop", ClipAnimPreset.OUT_SHRINK, 1.2f, 1.2f),
            CatalogAnimItem("Monitor Power Off", "Out", "Desktop", ClipAnimPreset.OUT_TV_OFF, 1.3f, 1.3f),
            CatalogAnimItem("Browser Close", "Out", "Desktop", ClipAnimPreset.OUT_SLIDE_DOWN, 1.15f, 1.2f),
            CatalogAnimItem("AE Motion Blur Out", "Out", "Desktop", ClipAnimPreset.OUT_SPIN_BLUR, 1.25f, 1.1f),
            CatalogAnimItem("3D Desktop Orbit", "Combo", "Desktop", ClipAnimPreset.COMBO_ORBIT_3D, 1.2f, 1.0f),
            CatalogAnimItem("Water Float Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_FLOATING, 1.15f, 0.85f),
            CatalogAnimItem("Player 1-2-3 Pulse", "Combo", "Desktop", ClipAnimPreset.COMBO_PULSE_BEAT, 1.25f, 1.2f),
            CatalogAnimItem("AE Motion Tile In", "In", "Desktop", ClipAnimPreset.IN_POP, 1.3f, 1.25f),
            CatalogAnimItem("3D Card Flip In", "In", "Desktop", ClipAnimPreset.IN_FLIP_X, 1.25f, 1.1f),
            CatalogAnimItem("Desktop Dock Zoom", "In", "Desktop", ClipAnimPreset.IN_ZOOM_FADE, 1.2f, 1.0f),
            CatalogAnimItem("AE Whip Pan Out", "Out", "Desktop", ClipAnimPreset.OUT_WHIP, 1.35f, 1.4f),
            CatalogAnimItem("3D Card Flip Out", "Out", "Desktop", ClipAnimPreset.OUT_FLIP_Y, 1.25f, 1.1f),
            CatalogAnimItem("Water Ripple Out", "Out", "Desktop", ClipAnimPreset.OUT_LIQUID, 1.2f, 1.0f),
            CatalogAnimItem("3D Cube Spin Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_3D_ZOOM, 1.3f, 1.15f),
            CatalogAnimItem("RSMB Shake Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_BASS_SHAKE, 1.35f, 1.3f),
            CatalogAnimItem("Reels Flash In", "In", "Desktop", ClipAnimPreset.IN_FLASH, 1.3f, 1.2f),
            CatalogAnimItem("Glitch Snap In", "In", "Desktop", ClipAnimPreset.IN_GLITCH, 1.35f, 1.3f),
            CatalogAnimItem("3D Cube Open", "In", "Desktop", ClipAnimPreset.IN_CUBE_ROT, 1.25f, 1.0f),
            CatalogAnimItem("Cinema Drop In", "In", "Desktop", ClipAnimPreset.IN_DROP, 1.25f, 1.1f),
            CatalogAnimItem("Shatter Break Out", "Out", "Desktop", ClipAnimPreset.OUT_SHATTER, 1.35f, 1.2f),
            CatalogAnimItem("Glitch Cut Out", "Out", "Desktop", ClipAnimPreset.OUT_GLITCH, 1.3f, 1.3f),
            CatalogAnimItem("Iris Cinema Close", "Out", "Desktop", ClipAnimPreset.OUT_IRIS_CLOSE, 1.2f, 1.0f),
            CatalogAnimItem("Curtain Sweep Out", "Out", "Desktop", ClipAnimPreset.OUT_CURTAIN, 1.2f, 1.1f),
            CatalogAnimItem("Vortex Spin Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_VORTEX, 1.3f, 1.15f),
            CatalogAnimItem("Elastic Pop Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_ELASTIC, 1.25f, 1.1f),
            CatalogAnimItem("Sonic Wave Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_SONIC_WAVE, 1.3f, 1.2f),
            CatalogAnimItem("Tilt Shift Combo", "Combo", "Desktop", ClipAnimPreset.COMBO_TILT_SHIFT, 1.2f, 1.0f)
        )
        list.addAll(desktopProAnims)
        val animMods = listOf(
            "Fast" to 1.8f, "Slow" to 0.6f, "Strong" to 1.6f,
            "2X" to 2.0f, "3X" to 3.0f, "5X" to 5.0f, "8X" to 8.0f, "10X" to 10.0f,
            "Snappy" to 2.4f, "Smooth" to 0.75f, "Bounce" to 2.8f, "Turbo" to 4.5f,
            "Hyper" to 6.5f, "Beat" to 3.2f, "Pro" to 1.5f, "Ultra" to 4.0f,
            "Gentle" to 0.5f, "Punch" to 3.6f, "Whip" to 5.2f, "Max" to 9.5f,
            "Mini" to 0.4f, "Power" to 4.2f, "Rapid" to 6.0f, "Dynamic" to 2.6f,
            "Explosive" to 8.5f, "Float" to 0.8f, "Snap" to 3.0f, "Extreme" to 9.0f,
            "Studio" to 1.3f, "Viral" to 2.2f, "Flash" to 5.0f, "Pop" to 2.5f
        )
        var count = 0
        for (preset in ClipAnimPreset.entries) {
            if (preset == ClipAnimPreset.NONE) continue
            for ((mod, valF) in animMods) {
                if (count >= 2900) break
                list.add(
                    CatalogAnimItem(
                        name = "${preset.label} $mod",
                        group = preset.group,
                        category = preset.category,
                        basePreset = preset,
                        defaultIntensity = if (mod == "Strong") valF else 1.0f,
                        defaultSpeed = if (mod != "Strong") valF else 1.0f,
                        isUniquePreset = false,
                        variationKind = "Modifier: $mod"
                    )
                )
                count++
            }
            if (count >= 2900) break
        }
        list
    }
}
