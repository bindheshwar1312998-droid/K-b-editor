package com.aistudio.kbeditor.r7v4qx.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AspectRatioPreset(val label: String, val widthRatio: Float, val heightRatio: Float) {
    RATIO_9_16("9:16", 9f, 16f),
    RATIO_16_9("16:9", 16f, 9f),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_4_5("4:5", 4f, 5f),
    RATIO_21_9("21:9", 21f, 9f)
}

@Serializable
enum class CanvasFillMode(val label: String) {
    FILL_CROP("Fill / Crop"),
    BLUR_CANVAS("Blur Canvas"),
    SOLID_COLOR("Solid Color")
}

@Serializable
enum class SpeedRampPreset(val label: String) {
    CONSTANT("Constant"),
    HERO_DIP("Hero Dip"),
    FLASH_IN("Flash In"),
    BULLET_TIME("Bullet Time"),
    BUILD_UP("Build Up")
}

@Serializable
enum class MaskType(val label: String) {
    NONE("None"),
    SPLIT_WIPE("Split"),
    ANAMORPHIC_BARS("Filmstrip"),
    CIRCLE("Circle"),
    ROUNDED_RECT("Rectangle"),
    MIRROR("Mirror"),
    HEART("Heart"),
    STAR("Star")
}

@Serializable
enum class MotionAnimation3D(val label: String) {
    NONE("Static"),
    POP_3D("Pop 3D"),
    FLIP_3D("Flip 3D"),
    FLOAT_NEON("Float Neon"),
    TYPEWRITER("Typewriter"),
    GLITCH_PULSE("Glitch Pulse"),
    SLIDE_KINETIC("Slide Kinetic")
}

@Serializable
enum class BlendModePreset(val label: String) {
    NORMAL("Normal"),
    SCREEN("Screen"),
    MULTIPLY("Multiply"),
    ADD("Add"),
    OVERLAY("Overlay")
}

@Serializable
enum class VideoFxPreset(val label: String, val category: String = "Trending") {
    NONE("None", "All"),
    // Trending (18)
    ZOOM_LENS("Zoom Lens", "Trending"),
    HALO_BLUR("Halo Blur", "Trending"),
    WHITE_FLASH("White Flash", "Trending"),
    GLITTER_BURST("Glitter Burst", "Trending"),
    ZOOM_PULSE("Zoom Beat", "Trending"),
    NEON_EDGE("Neon Edge", "Trending"),
    EARTHQUAKE("Earthquake", "Trending"),
    ZOOM_FLASH("Zoom Flash", "Trending"),
    PIZZAZZ_2("Pizzazz 2", "Trending"),
    BLACK_FLASH_2("Black Flash 2", "Trending"),
    EXPLODING_LOVE("Exploding Love", "Trending"),
    SILK_SHARPEN("Silk Sharpen", "Trending"),
    ENERGY_CORE("Energy Core", "Trending"),
    QUANTUM_PULSE("Quantum Pulse", "Trending"),
    SATIN_REVEAL("Satin Reveal", "Trending"),
    SLASH_REVEAL("Slash Reveal", "Trending"),
    LIMELIGHT_MOTION("Limelight Motion", "Trending"),
    FLUORESCENCE("Fluorescence", "Trending"),
    // Lens & Blur (20)
    FADE_IN_FX("Fade In", "Lens & Blur"),
    FADE_OUT_FX("Fade Out", "Lens & Blur"),
    MINI_ZOOM("Mini Zoom", "Lens & Blur"),
    ZOOM_OUT_FX("Zoom Out", "Lens & Blur"),
    LIGHT_BOKEH("Light Bokeh", "Lens & Blur"),
    SOFT_GLOW("Divine Glow", "Lens & Blur"),
    VIGNETTE_SHOT("Vignette Shot", "Lens & Blur"),
    DIAMOND_BLUR("Diamond Blur", "Lens & Blur"),
    CHROME_BLUR("Chrome Blur", "Lens & Blur"),
    SMOKY_FOCUS("Smoky Focus", "Lens & Blur"),
    TILT_ZOOM_IN("Tilt Zoom-In", "Lens & Blur"),
    SOFT_ZOOM("Soft Zoom", "Lens & Blur"),
    ZOOM_CURSOR("Zoom Cursor", "Lens & Blur"),
    PORTRAIT_OPEN("Portrait Open", "Lens & Blur"),
    MOTION_BLUR("Motion Blur", "Lens & Blur"),
    RADIAL_BLUR("Radial Blur", "Lens & Blur"),
    OBLIQUE_BLUR("Oblique Blur", "Lens & Blur"),
    FUZZY_HALO("Fuzzy Halo", "Lens & Blur"),
    PORTRAIT_FOCUS("Portrait Focus", "Lens & Blur"),
    MACRO_SHARPEN("Macro Sharpen", "Lens & Blur"),
    // Retro & Film (20)
    VHS_RETRO("VHS Retro", "Retro & Film"),
    OLD_TV("Old TV CRT", "Retro & Film"),
    CINEMA_GRAIN("Film Grain", "Retro & Film"),
    VINTAGE_FILM("Vintage Film", "Retro & Film"),
    BW_GRAIN("B&W Grain", "Retro & Film"),
    RETRO_FLICKER("Retro Flicker", "Retro & Film"),
    RETRO_CINEMA("Retro Cinema", "Retro & Film"),
    BW_SCANNER("B&W Scanner", "Retro & Film"),
    VIDEO_CAM("Video Cam", "Retro & Film"),
    LOW_EXPOSURE("Low Exposure", "Retro & Film"),
    DARK_FADE("Dark Fade", "Retro & Film"),
    SEPIA_NOIR("Sepia Noir", "Retro & Film"),
    LEAK_1("Light Leak 1", "Retro & Film"),
    LEAK_2("Light Leak 2", "Retro & Film"),
    SUPER_8MM("Super 8mm", "Retro & Film"),
    KODAK_WARM("Kodak Warm", "Retro & Film"),
    TAPE_REWIND("Tape Rewind", "Retro & Film"),
    NOIR_SHADOW("Noir Shadow", "Retro & Film"),
    DUST_SCRATCH("Dust & Scratch", "Retro & Film"),
    POLAROID_FADE("Polaroid Fade", "Retro & Film"),
    // Shake & Beat (20)
    CAM_SHAKE("Cam Shake", "Shake & Beat"),
    HEARTBEAT("Heartbeat", "Shake & Beat"),
    CAM_SWING("Cam Swing", "Shake & Beat"),
    RING_PULSE("Ring Pulse", "Shake & Beat"),
    BOUNCE_FX("Bounce", "Shake & Beat"),
    BOUNCING_SNOW("Bouncing Beat", "Shake & Beat"),
    REBOUND_SWING("Rebound Swing", "Shake & Beat"),
    SINGLE_SHOCK("Single Shock", "Shake & Beat"),
    PHONE_SWAG("Phone Swag", "Shake & Beat"),
    SLIGHT_SHAKE("Slight Shake", "Shake & Beat"),
    DOODLE_PULSE("Doodle Pulse", "Shake & Beat"),
    RADIANT_PULSE("Radiant Pulse", "Shake & Beat"),
    METEOR_CURVE("Meteor Curve", "Shake & Beat"),
    PENDULUM_SWING("Pendulum", "Shake & Beat"),
    BASS_DROP("Bass Drop", "Shake & Beat"),
    SHOCKWAVE_BEAT("Shockwave Beat", "Shake & Beat"),
    VERTICAL_JOLT("Vertical Jolt", "Shake & Beat"),
    HORIZONTAL_SLAP("Horizontal Slap", "Shake & Beat"),
    DRIFT_WOBBLE("Drift Wobble", "Shake & Beat"),
    RAPID_ZOOM("Rapid Zoom", "Shake & Beat"),
    // Light & Sparkle (20)
    FLASH_STROBE("Flash Strobe", "Light & Sparkle"),
    ACTION_FLASH("Action Flash", "Light & Sparkle"),
    DREAM_BLOOM("Dream Glow", "Light & Sparkle"),
    STARLIGHT("Starlight", "Light & Sparkle"),
    GOLD_SEQUINS("Gold Sequins", "Light & Sparkle"),
    SPARKLE_CRASH("Sparkle Crash", "Light & Sparkle"),
    STAR_RAIN("Star Rain", "Light & Sparkle"),
    STAR_POWER("Star Power", "Light & Sparkle"),
    FIREFLY_LIGHTS("Firefly Lights", "Light & Sparkle"),
    SOFT_GLITTER("Soft Glitter", "Light & Sparkle"),
    TWINKLE_DUST("Twinkle Dust", "Light & Sparkle"),
    GOLDEN_MAGIC("Golden Magic", "Light & Sparkle"),
    LIGHT_SWEEP("Light Sweep", "Light & Sparkle"),
    FAINT_SHEEN("Faint Sheen", "Light & Sparkle"),
    GLITTER_FALL("Glitter Fall", "Light & Sparkle"),
    BLING_RING("Bling Ring", "Light & Sparkle"),
    DIAMOND_SPARKLE("Diamond Sparkle", "Light & Sparkle"),
    SUNSET_FLARE("Sunset Flare", "Light & Sparkle"),
    PRISM_RAY("Prism Ray", "Light & Sparkle"),
    AURORA_GLOW("Aurora Glow", "Light & Sparkle"),
    // Split & Glitch (18)
    GLITCH_RGB("RGB Glitch", "Split & Glitch"),
    CHROMATIC("Chromatic", "Split & Glitch"),
    SPLIT_2X("2-Split", "Split & Glitch"),
    SPLIT_3X("3-Split", "Split & Glitch"),
    SPLIT_4X("4-Split", "Split & Glitch"),
    SPLIT_6X("6-Split Grid", "Split & Glitch"),
    SPLIT_9X("9-Split Grid", "Split & Glitch"),
    SLICE_BY_SLICE("Slice By Slice", "Split & Glitch"),
    TEAR_APART("Tear Apart", "Split & Glitch"),
    SHARDS_LIGHT("Shards of Light", "Split & Glitch"),
    PRISM_SHIMMER("Prism Shimmer", "Split & Glitch"),
    LASER_WIPE("Laser Wipe", "Split & Glitch"),
    MIRROR_SPLIT("Mirror Split", "Split & Glitch"),
    KALEIDOSCOPE("Kaleidoscope", "Split & Glitch"),
    RGB_WAVE("RGB Wave", "Split & Glitch"),
    SIGNAL_LOST("Signal Lost", "Split & Glitch"),
    STATIC_NOISE("Static Noise", "Split & Glitch"),
    DATAMOSH("Datamosh", "Split & Glitch"),
    // Nightclub (16)
    NEON_FLOW("Neon Flow", "Nightclub"),
    BLAZE_BURST("Blaze Burst", "Nightclub"),
    LASER_REVEAL("Laser Reveal", "Nightclub"),
    FLASH_BOUNCE("Flash Bounce", "Nightclub"),
    CAMERA_STROBE("Camera Strobe", "Nightclub"),
    DECOLOR_FLASH("Decolor Flash", "Nightclub"),
    CYBER_SCAN("Cyber Scan", "Nightclub"),
    FLICKER_BLACK("Flicker Black", "Nightclub"),
    STELLAR_PARTY("Stellar Party", "Nightclub"),
    CLUB_SPOTLIGHT("Club Spotlight", "Nightclub"),
    DISCO_BALL("Disco Ball", "Nightclub"),
    UV_BLACKLIGHT("UV Blacklight", "Nightclub"),
    LASER_GRID("Laser Grid", "Nightclub"),
    DJ_STROBE("DJ Strobe", "Nightclub"),
    NEON_PULSE("Neon Pulse", "Nightclub"),
    RAVE_FLASH("Rave Flash", "Nightclub"),
    // Comic & Manga (14)
    MANGA_SHOCK("Manga Shock", "Comic & Manga"),
    INK_SPILL("Ink Spill", "Comic & Manga"),
    CLAYMATION("Claymation", "Comic & Manga"),
    PIXEL_MOSAIC("Pixel Mosaic", "Comic & Manga"),
    SKETCH_OUTLINE("Sketch Line", "Comic & Manga"),
    POP_ART("Pop Art", "Comic & Manga"),
    DOODLE_FRAME("Doodle Frame", "Comic & Manga"),
    HALFTONE_COMIC("Halftone Comic", "Comic & Manga"),
    ANIME_SPEEDLINES("Speed Lines", "Comic & Manga"),
    WATERCOLOR_ART("Watercolor", "Comic & Manga"),
    OIL_PAINT("Oil Paint", "Comic & Manga"),
    NEON_SKETCH("Neon Sketch", "Comic & Manga"),
    CHALK_DRAW("Chalk Draw", "Comic & Manga"),
    PENCIL_SKETCH("Pencil Sketch", "Comic & Manga"),
    // Nature & Party (14)
    PETAL_PATH("Petal Path", "Nature & Party"),
    LEAF_CURTAINS("Leaf Curtains", "Nature & Party"),
    FALLEN_LEAVES("Fallen Leaves", "Nature & Party"),
    CONFETTI_BLOW("Confetti Blow", "Nature & Party"),
    BUBBLES_FX("Bubbles", "Nature & Party"),
    SNOW_CLEARING("Snowfall", "Nature & Party"),
    AUTUMN_WIND("Autumn Wind", "Nature & Party"),
    SAKURA_CARD("Sakura Card", "Nature & Party"),
    RAIN_DROPS("Rain Drops", "Nature & Party"),
    THUNDER_LIGHT("Thunder Flash", "Nature & Party"),
    FIREWORKS_FX("Fireworks", "Nature & Party"),
    ROSE_PETALS("Rose Petals", "Nature & Party"),
    GOLD_CONFETTI("Gold Confetti", "Nature & Party"),
    SUNBEAM_RAYS("Sunbeam Rays", "Nature & Party"),
    // Opening & Closing (14)
    OPENING_ARC("Opening Arc", "Opening & Closing"),
    CIRCLE_OPENING("Circle Open", "Opening & Closing"),
    VERTICAL_OPEN("Vertical Open", "Opening & Closing"),
    HORIZONTAL_OPEN("Horizontal Open", "Opening & Closing"),
    FANCY_CURTAIN("Fancy Curtain", "Opening & Closing"),
    THE_END_FX("The End", "Opening & Closing"),
    CANVAS_FLIP("Canvas Flip", "Opening & Closing"),
    SPIRAL_REVEAL("Spiral Reveal", "Opening & Closing"),
    CINEMA_BARS("Cinema Bars", "Opening & Closing"),
    SHUTTER_CLOSE("Shutter Close", "Opening & Closing"),
    BLINDS_OPEN("Blinds Open", "Opening & Closing"),
    IRIS_CLOSE("Iris Close", "Opening & Closing"),
    TV_POWER_OFF("TV Power Off", "Opening & Closing"),
    TILT_EXPAND("Tilted Expand", "Opening & Closing"),
    // Love & Aesthetic (14)
    HEART_SODA("Heart Soda", "Love & Aesthetic"),
    ROMANTIC_SPARK("Romantic Spark", "Love & Aesthetic"),
    CUTE_HEARTS("Cute Hearts", "Love & Aesthetic"),
    PINK_DREAM("Pink Dream", "Love & Aesthetic"),
    SOFT_PASTEL("Soft Pastel", "Love & Aesthetic"),
    LOVE_BLUSH("Love Blush", "Love & Aesthetic"),
    BUTTERFLY_LOVE("Butterfly Love", "Love & Aesthetic"),
    PEARL_GLOW("Pearl Glow", "Love & Aesthetic"),
    ANGELIC_TWIN("Angelic Glow", "Love & Aesthetic"),
    DREAMY_SUNSET("Dreamy Sunset", "Love & Aesthetic"),
    CREAM_TONE("Cream Tone", "Love & Aesthetic"),
    VELVET_ROSE("Velvet Rose", "Love & Aesthetic"),
    CLOUD_SOFT("Cloud Soft", "Love & Aesthetic"),
    FAIRY_DUST("Fairy Dust", "Love & Aesthetic"),
    // Cyber & Sci-Fi (14)
    MATRIX_CODE("Matrix Code", "Cyber & Sci-Fi"),
    HOLOGRAM_3D("Hologram 3D", "Cyber & Sci-Fi"),
    SPACE_TRAVEL("Space Travel", "Cyber & Sci-Fi"),
    PORTAL_WARP("Portal Warp", "Cyber & Sci-Fi"),
    CYBER_GLITCH("Cyber Glitch", "Cyber & Sci-Fi"),
    HUD_SCANNER("HUD Scanner", "Cyber & Sci-Fi"),
    NEON_GRID("Neon Grid", "Cyber & Sci-Fi"),
    HYPER_DRIVE("Hyper Drive", "Cyber & Sci-Fi"),
    QUANTUM_SHIFT("Quantum Shift", "Cyber & Sci-Fi"),
    FUTURE_VISOR("Future Visor", "Cyber & Sci-Fi"),
    DIGITAL_RAIN("Digital Rain", "Cyber & Sci-Fi"),
    PLASMA_EDGE("Plasma Edge", "Cyber & Sci-Fi"),
    LASER_BEAM("Laser Beam", "Cyber & Sci-Fi"),
    XRAY_INVERT("X-Ray Invert", "Cyber & Sci-Fi"),
    // Cinema & Moody (14)
    TEAL_ORANGE_FX("Teal & Orange", "Cinema & Moody"),
    DARK_GOTHAM("Dark Gotham", "Cinema & Moody"),
    MOODY_RAIN("Moody Rain", "Cinema & Moody"),
    GOLDEN_HOUR("Golden Hour", "Cinema & Moody"),
    SILVER_HALIDE("Silver Halide", "Cinema & Moody"),
    BLEACH_BYPASS("Bleach Bypass", "Cinema & Moody"),
    ANAMORPHIC_FLARE("Anamorphic Flare", "Cinema & Moody"),
    MIDNIGHT_BLUE("Midnight Blue", "Cinema & Moody"),
    DRAMATIC_CONTRAST("Dramatic Contrast", "Cinema & Moody"),
    FOGGY_MIST("Foggy Mist", "Cinema & Moody"),
    SHADOW_VIGNETTE("Shadow Vignette", "Cinema & Moody"),
    CRISP_HDR("Crisp HDR", "Cinema & Moody"),
    MONOCHROME_PRO("Monochrome Pro", "Cinema & Moody"),
    BLOCKBUSTER("Blockbuster", "Cinema & Moody"),
    // Action & Sports (12)
    SPEED_RAMP_FX("Speed Rush", "Action & Sports"),
    MOTION_TRAIL("Motion Trail", "Action & Sports"),
    IMPACT_SHAKE("Impact Shake", "Action & Sports"),
    TURBO_BLUR("Turbo Blur", "Action & Sports"),
    WHIP_PAN_FX("Whip Pan", "Action & Sports"),
    DRIFT_ZOOM("Drift Zoom", "Action & Sports"),
    ARENA_LIGHTS("Arena Lights", "Action & Sports"),
    CLUTCH_FLASH("Clutch Flash", "Action & Sports"),
    SLOWMO_TRAIL("Slow-Mo Trail", "Action & Sports"),
    ADRENALINE("Adrenaline", "Action & Sports"),
    NITRO_BOOST("Nitro Boost", "Action & Sports"),
    KNOCKOUT_HIT("Knockout Hit", "Action & Sports"),
    // Desktop, Computer & Player 1.2.3
    PC_MONITOR("Desktop Screen", "Desktop & Player"),
    WINDOWS_XP("Retro PC Win", "Desktop & Player"),
    MAC_WINDOW("Mac Window", "Desktop & Player"),
    TERMINAL_HUD("Hacker Terminal", "Desktop & Player"),
    MEDIA_PLAYER_UI("Player 1.2.3 UI", "Desktop & Player"),
    COUNTDOWN_321("3-2-1 Countdown", "Desktop & Player"),
    // Water Reflection & 3D Effects
    WATER_REFLECTION("Water Reflection", "Water & 3D"),
    WATER_RIPPLE("Water Ripple", "Water & 3D"),
    RAIN_PUDDLE("Rain Puddle", "Water & 3D"),
    MIRROR_LAKE("Mirror Lake", "Water & 3D"),
    DEPTH_3D_CUBE("3D Cube", "Water & 3D"),
    PARALLAX_3D("3D Parallax", "Water & 3D"),
    PERSPECTIVE_3D("3D Tilt", "Water & 3D"),
    TUNNEL_3D("3D Tunnel", "Water & 3D"),
    // Viral Multi-Photo Reel Templates
    PAGE_FLIP_ALBUM("Page Flip Album", "Reels & Viral"),
    CAROUSEL_3D("3D Curved Album", "Reels & Viral"),
    CLOCK_DIAL_4("Clock 4-Photo", "Reels & Viral"),
    SPLIT_FILMSTRIP("Split Filmstrip", "Reels & Viral"),
    INFINITE_TUNNEL("Infinite Tunnel", "Reels & Viral"),
    DIARY_FRAME_2("Diary Frame", "Reels & Viral")
}


@Serializable
enum class ClipAnimPreset(val label: String, val group: String, val category: String) {
    NONE("None", "All", "Basic"),
    // IN (24)
    IN_FADE("Fade In", "In", "Basic"),
    IN_ZOOM_1("Zoom 1", "In", "Trending"),
    IN_ZOOM_IN("Zoom In", "In", "Basic"),
    IN_MINI_ZOOM("Mini Zoom", "In", "Basic"),
    IN_SLIDE_RIGHT("Slide Right", "In", "Basic"),
    IN_SLIDE_LEFT("Slide Left", "In", "Basic"),
    IN_SLIDE_UP("Slide Up", "In", "Basic"),
    IN_SLIDE_DOWN("Slide Down", "In", "Basic"),
    IN_FLIP("Flip In", "In", "3D"),
    IN_SPIN("Spin In", "In", "3D"),
    IN_BLUR("Blur In", "In", "Basic"),
    IN_GLOW_FADE("Glow Fade-In", "In", "Light"),
    IN_ROCK_VERT("Rock Vertical", "In", "Trending"),
    IN_SHAKE_1("Shake 1", "In", "Vibration"),
    IN_SHAKE_2("Shake 2", "In", "Vibration"),
    IN_SCANLINE("Scanline In", "In", "Glitch"),
    IN_CIRCLE_OPEN("Circle Open", "In", "Mask"),
    IN_UNFOLD("Unfold", "In", "3D"),
    IN_3D_FORM("3D Formation", "In", "3D"),
    IN_CUBE_ROT("Cube Rotate", "In", "3D"),
    IN_SUDDEN_IMPACT("Sudden Impact", "In", "Vibration"),
    IN_BEAT_BOUNCE("Beat Bounce", "In", "Vibration"),
    IN_BASS_PULSE("Bass Pulse", "In", "Vibration"),
    IN_LIGHT_PULSE("Light Pulse", "In", "Light"),
    // OUT (20)
    OUT_FADE("Fade Out", "Out", "Basic"),
    OUT_ZOOM("Zoom Out", "Out", "Basic"),
    OUT_ZOOM_BACK("Zoom Back", "Out", "Basic"),
    OUT_SLIDE_LEFT("Slide Out L", "Out", "Basic"),
    OUT_SLIDE_RIGHT("Slide Out R", "Out", "Basic"),
    OUT_SLIDE_UP("Slide Out Up", "Out", "Basic"),
    OUT_SLIDE_DOWN("Slide Out Dn", "Out", "Basic"),
    OUT_FLIP("Flip Out", "Out", "3D"),
    OUT_SPIN("Spin Out", "Out", "3D"),
    OUT_BLUR("Blur Out", "Out", "Basic"),
    OUT_SHRINK("Shrink Out", "Out", "Basic"),
    OUT_IRIS_CLOSE("Iris Close", "Out", "Mask"),
    OUT_SHAKE("Shake Out", "Out", "Vibration"),
    OUT_STORM_FLASH("Storm Flash", "Out", "Light"),
    OUT_JITTER("Jitter Out", "Out", "Vibration"),
    OUT_LIQUID("Liquid Out", "Out", "Glitch"),
    OUT_CUBE("Cube Out", "Out", "3D"),
    OUT_CURTAIN("Curtain Close", "Out", "Mask"),
    OUT_TV_OFF("TV Power Off", "Out", "Glitch"),
    OUT_SKEW("Skew Out", "Out", "3D"),
    // COMBO (20)
    COMBO_PENDULUM("Pendulum", "Combo", "Trending"),
    COMBO_DISTORT_SPIN("Distort Spin", "Combo", "3D"),
    COMBO_3D_ZOOM("3D Zoom", "Combo", "3D"),
    COMBO_BOUNCE("Bounce", "Combo", "Vibration"),
    COMBO_ELASTIC("Elastic", "Combo", "Basic"),
    COMBO_WOBBLE("Wobble", "Combo", "Vibration"),
    COMBO_VORTEX("Infinity Vortex", "Combo", "Trending"),
    COMBO_ZOOM_SPIN("Zoom & Spin", "Combo", "3D"),
    COMBO_QUICK_SPIN("Quick Spin", "Combo", "3D"),
    COMBO_SONIC_WAVE("Sonic Wave", "Combo", "Vibration"),
    COMBO_WHIRL("Whirl", "Combo", "Trending"),
    COMBO_SWING_BOT("Swing Bottom", "Combo", "Basic"),
    COMBO_SKEW_ZOOM("Skew Zoom", "Combo", "3D"),
    COMBO_STRETCH_FALL("Stretch Fall", "Combo", "Vibration"),
    COMBO_NEON_POKER("Neon Poker", "Combo", "3D"),
    COMBO_RISE_FLIP("Rise & Flip", "Combo", "3D"),
    COMBO_FLOATING("Floating", "Combo", "Light"),
    COMBO_HEARTBEAT("Heartbeat", "Combo", "Vibration"),
    COMBO_BASS_SHAKE("Bass Shake", "Combo", "Vibration"),
    COMBO_GLITCH("Glitch Combo", "Combo", "Glitch"),
    // Extra 26 CapCut-style In / Out / Combo Animations (90 total)
    IN_POP("Pop In", "In", "Trending"),
    IN_WHIP("Whip In", "In", "Trending"),
    IN_GLITCH("Glitch In", "In", "Glitch"),
    IN_FLASH("Flash In", "In", "Light"),
    IN_ROLL("Roll In", "In", "3D"),
    IN_DROP("Drop In", "In", "Basic"),
    IN_FLIP_X("3D Flip X", "In", "3D"),
    IN_SWING_L("Swing Left", "In", "Basic"),
    IN_ZOOM_FADE("Zoom & Fade", "In", "Basic"),
    OUT_POP("Pop Out", "Out", "Trending"),
    OUT_WHIP("Whip Out", "Out", "Trending"),
    OUT_GLITCH("Glitch Out", "Out", "Glitch"),
    OUT_FLASH("Flash Out", "Out", "Light"),
    OUT_ROLL("Roll Out", "Out", "3D"),
    OUT_DROP("Drop Out", "Out", "Basic"),
    OUT_FLIP_Y("3D Flip Y", "Out", "3D"),
    OUT_SWING_R("Swing Right", "Out", "Basic"),
    OUT_SHATTER("Shatter Out", "Out", "Glitch"),
    OUT_SPIN_BLUR("Spin & Blur", "Out", "3D"),
    COMBO_ORBIT_3D("Orbit 3D", "Combo", "3D"),
    COMBO_WAVE("Wave Combo", "Combo", "Light"),
    COMBO_SHAKE_3("Shake 3", "Combo", "Vibration"),
    COMBO_PULSE_BEAT("Pulse Beat", "Combo", "Vibration"),
    COMBO_TILT_SHIFT("Tilt Shift", "Combo", "3D"),
    COMBO_SPIN_BLUR("Spin Blur", "Combo", "3D"),
    COMBO_SWING_R("Swing Right", "Combo", "Basic")
}

@Serializable
enum class TransitionType(val label: String) {
    NONE("Cut"),
    CROSS_DISSOLVE("Dissolve"),
    WHIP_PAN("Whip Pan"),
    FLASH_CUT("Flash"),
    ZOOM_THROUGH("Zoom")
}

@Serializable
enum class VocalIsolationMode(val label: String) {
    FULL_MIX("Full Mix"),
    ISOLATE_VOCALS("Vocals Only"),
    INSTRUMENTAL_ONLY("Instrumental")
}

@Serializable
enum class ExportResolution(val label: String, val widthPx: Int, val heightPx: Int) {
    HD_720("720p", 1280, 720),
    FHD_1080("1080p", 1920, 1080),
    UHD_4K("4K UHD", 3840, 2160)
}

@Serializable
enum class ExportFps(val value: Int, val label: String) {
    FPS_24(24, "24 fps"),
    FPS_30(30, "30 fps"),
    FPS_60(60, "60 fps")
}

@Serializable
enum class ExportDynamicRange(val label: String) {
    SDR_8BIT("SDR 8-bit (H.264)"),
    HDR10_HLG("HDR10 HLG (10-bit HEVC)")
}

@Serializable
data class ColorWheelPoint(
    val dx: Float = 0f,
    val dy: Float = 0f,
    val luma: Float = 0f
)

@Serializable
data class ColorGradeSpec(
    val lift: ColorWheelPoint = ColorWheelPoint(),
    val gamma: ColorWheelPoint = ColorWheelPoint(),
    val gain: ColorWheelPoint = ColorWheelPoint(),
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vignette: Float = 0f,
    val beautySmooth: Float = 0f,
    val beautyGlow: Float = 0f,
    val vibrance: Float = 0f,
    val lutName: String = "Neutral"
)

@Serializable
data class BrushEraserPoint(
    val x: Float,
    val y: Float,
    val radius: Float = 0.045f
)

@Serializable
data class WatermarkRemoverSpec(
    val enabled: Boolean = false,
    val posX: Float = 0.84f,
    val posY: Float = 0.12f,
    val boxWidth: Float = 0.22f,
    val boxHeight: Float = 0.08f,
    val cropMode: Boolean = false
)

@Serializable
data class FaceSwapSpec(
    val enabled: Boolean = false,
    val sourceFaceUri: String = "",
    val scaleMultiplier: Float = 1.15f,
    val blendOpacity: Float = 0.94f
)

@Serializable
data class ChromaKeySpec(
    val enabled: Boolean = false,
    val keyHue: String = "Green",
    val tolerance: Float = 0.25f,
    val spillSuppression: Float = 0.4f,
    val aiAutoCut: Boolean = false,
    val studioBackdropHex: Long = 0xFF0F172A
)

@Serializable
data class KeyframePoint(
    val normalizedTime: Float,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val easingCurve: String = "Ease In-Out"
)

@Serializable
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Clip",
    val mediaUri: String = "",
    val proxyUri: String = "",
    val isAiGeneratedBroll: Boolean = false,
    val sourceDurationMs: Long = 6000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 6000L,
    val slipOffsetMs: Long = 0L,
    val speedMultiplier: Float = 1f,
    val speedRamp: SpeedRampPreset = SpeedRampPreset.CONSTANT,
    val opticalFlowEnabled: Boolean = true,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val colorGrade: ColorGradeSpec = ColorGradeSpec(),
    val maskType: MaskType = MaskType.NONE,
    val maskFeather: Float = 0.08f,
    val maskRotationDeg: Float = 0f,
    val maskInverted: Boolean = false,
    val maskScale: Float = 0.85f,
    val maskScaleX: Float = 1.0f,
    val maskScaleY: Float = 1.0f,
    val maskOffsetX: Float = 0f,
    val maskOffsetY: Float = 0f,
    val flipHorizontal: Boolean = false,
    val rotationDeg: Float = 0f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val zoomScale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val chromaKey: ChromaKeySpec = ChromaKeySpec(),
    val faceSwap: FaceSwapSpec = FaceSwapSpec(),
    val watermarkRemover: WatermarkRemoverSpec = WatermarkRemoverSpec(),
    val magicEraserPoints: List<BrushEraserPoint> = emptyList(),
    val keyframes: List<KeyframePoint> = emptyList(),
    val motionTrackPoints: List<KeyframePoint> = emptyList(),
    val motionTrackMode: String = "2D",
    val transitionOut: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 400L,
    val videoFx: VideoFxPreset = VideoFxPreset.NONE,
    val animIn: ClipAnimPreset = ClipAnimPreset.NONE,
    val animOut: ClipAnimPreset = ClipAnimPreset.NONE,
    val animCombo: ClipAnimPreset = ClipAnimPreset.NONE,
    val animDurationMs: Long = 600L,
    val animIntensity: Float = 1.0f,
    val animSpeed: Float = 1.0f,
    val videoFxIntensity: Float = 1.0f,
    val videoFxTexture: Float = 0.45f,
    val videoFxColorShift: Float = 0.0f,
    val videoFxSpeed: Float = 1.0f,
    val videoFxStartMs: Long = 0L,
    val videoFxEndMs: Long = -1L,
    val filterIntensity: Float = 1.0f,
    val videoFxLabel: String = "",
    val animLabel: String = "",
    val reframeOffsetX: Float = 0f
) {
    val activeDurationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(300L) / speedMultiplier.coerceAtLeast(0.1f)).toLong()
}

@Serializable
data class OverlayPipClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "PiP Overlay",
    val mediaUri: String = "",
    val opticalFxPreset: String? = null,
    val startMs: Long = 0L,
    val durationMs: Long = 3500L,
    val blendMode: BlendModePreset = BlendModePreset.SCREEN,
    val opacity: Float = 0.85f,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scale: Float = 0.55f,
    val rotationDeg: Float = 0f
)

@Serializable
enum class CaptionLanguage(val label: String, val code: String) {
    AUTO_DETECT("Auto Detect", "auto"),
    HINDI("हिन्दी (Hindi)", "hi"),
    HINGLISH("Hinglish", "hi-Latn"),
    ENGLISH("English", "en"),
    PUNJABI("ਪੰਜਾਬੀ (Punjabi)", "pa"),
    BHOJPURI("भोजपुरी (Bhojpuri)", "bho"),
    HARYANVI("हरियाणवी (Haryanvi)", "bgc"),
    RAJASTHANI("राजस्थानी (Rajasthani)", "raj"),
    MARWARI("मारवाड़ी (Marwari)", "mwr"),
    AWADHI("अवधी (Awadhi)", "awa"),
    CHHATTISGARHI("छत्तीसगढ़ी", "hne"),
    PAHADI("पहाड़ी / गढ़वाली", "gbm"),
    MAGAHI("मगही (Magahi)", "mag"),
    BENGALI("বাংলা (Bengali)", "bn"),
    MARATHI("मराठी (Marathi)", "mr"),
    TELUGU("తెలుగు (Telugu)", "te"),
    TAMIL("தமிழ் (Tamil)", "ta"),
    GUJARATI("ગુજરાતી (Gujarati)", "gu"),
    URDU("اردو (Urdu)", "ur"),
    KANNADA("ಕನ್ನಡ (Kannada)", "kn"),
    MALAYALAM("മലയാളം (Malayalam)", "ml"),
    ODIA("ଓଡ଼ିଆ (Odia)", "or"),
    ASSAMESE("অসমীয়া (Assamese)", "as"),
    MAITHILI("मैथिली (Maithili)", "mai"),
    SANTALI("ᱥᱟᱱᱛᱟᱲᱤ (Santali)", "sat"),
    KASHMIRI("कॉशुर (Kashmiri)", "ks"),
    NEPALI("नेपाली (Nepali)", "ne"),
    SINDHI("सिंधी (Sindhi)", "sd"),
    KONKANI("कोंकणी (Konkani)", "kok"),
    DOGRI("डोगरी (Dogri)", "doi"),
    MANIPURI("মৈতৈলোন্ (Manipuri)", "mni"),
    BODO("बड़ो (Bodo)", "brx"),
    SANSKRIT("संस्कृतम् (Sanskrit)", "sa"),
    TULU("ತುಳು (Tulu)", "tcy"),
    SPANISH("Español (Spanish)", "es"),
    ARABIC("العربية (Arabic)", "ar"),
    FRENCH("Français (French)", "fr"),
    GERMAN("Deutsch (German)", "de"),
    CHINESE_SIMPLIFIED("中文 (Chinese)", "zh"),
    JAPANESE("日本語 (Japanese)", "ja"),
    KOREAN("한국어 (Korean)", "ko"),
    PORTUGUESE("Português (Portuguese)", "pt"),
    RUSSIAN("Русский (Russian)", "ru"),
    ITALIAN("Italiano (Italian)", "it"),
    TURKISH("Türkçe (Turkish)", "tr"),
    INDONESIAN("Bahasa Indonesia", "id"),
    MALAY("Bahasa Melayu", "ms"),
    THAI("ไทย (Thai)", "th"),
    VIETNAMESE("Tiếng Việt", "vi"),
    PERSIAN("فارسی (Persian)", "fa"),
    DUTCH("Nederlands (Dutch)", "nl"),
    POLISH("Polski (Polish)", "pl"),
    UKRAINIAN("Українська", "uk"),
    SWAHILI("Kiswahili", "sw"),
    GREEK("Ελληνικά (Greek)", "el"),
    SWEDISH("Svenska (Swedish)", "sv"),
    NORWEGIAN("Norsk (Norwegian)", "no"),
    DANISH("Dansk (Danish)", "da"),
    FINNISH("Suomi (Finnish)", "fi"),
    CZECH("Čeština (Czech)", "cs"),
    ROMANIAN("Română (Romanian)", "ro"),
    HUNGARIAN("Magyar (Hungarian)", "hu"),
    HEBREW("עברית (Hebrew)", "he"),
    FILIPINO("Filipino / Tagalog", "tl"),
    SINHALA("සිංහල (Sinhala)", "si"),
    BURMESE("မြန်မာ (Burmese)", "my")
}

@Serializable
enum class CaptionFontStyle(val label: String) {
    SANS_BOLD("Modern Sans"),
    SERIF_EDITORIAL("Luxury Serif"),
    MONO_CODE("Tech Mono"),
    ROUNDED_POP("Rounded Pop")
}

@Serializable
enum class CaptionStylePreset(val label: String) {
    VIRAL_HORMOZI("Viral Bold"),
    NEON_CYBER("Neon Glow"),
    DARK_PILL("Dark Pill"),
    GRADIENT_POP("Gradient Pop"),
    GOLD_LUXURY("Gold Luxury"),
    OUTLINE_IMPACT("Outline Pro"),
    CINEMA_SUB("Cinema Sub"),
    MINIMAL_TAG("Minimal")
}

@Serializable
data class CaptionClip(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "KB EDITOR PRO",
    val startMs: Long = 0L,
    val durationMs: Long = 3000L,
    val animation3D: MotionAnimation3D = MotionAnimation3D.POP_3D,
    val stylePreset: CaptionStylePreset = CaptionStylePreset.VIRAL_HORMOZI,
    val fontStyle: CaptionFontStyle = CaptionFontStyle.SANS_BOLD,
    val rotationDeg: Float = 0f,
    val languageCode: String = "auto",
    val posX: Float = 0.5f,
    val posY: Float = 0.8f,
    val fontSizeSp: Float = 22f,
    val colorHex: Long = 0xFFF8FAFC,
    val accentHex: Long = 0xFFF59E0B,
    val bgAlpha: Float = 0.72f,
    val isBold: Boolean = true,
    val trackedToClipId: String? = null
)

@Serializable
data class AudioClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Audio Track",
    val mediaUri: String = "",
    val startMs: Long = 0L,
    val durationMs: Long = 6000L,
    val volume: Float = 1f,
    val windFilterEnabled: Boolean = false,
    val noiseGateEnabled: Boolean = false,
    val noiseReductionStrength: Float = 0.75f,
    val voiceEnhanceEnabled: Boolean = false,
    val echoRemovalEnabled: Boolean = false,
    val vocalMode: VocalIsolationMode = VocalIsolationMode.FULL_MIX,
    val beatMarkersMs: List<Long> = emptyList(),
    val isVoiceover: Boolean = false
)

@Serializable
data class TrackState(
    val v1Locked: Boolean = false,
    val v1Visible: Boolean = true,
    val o1Locked: Boolean = false,
    val o1Visible: Boolean = true,
    val t1Locked: Boolean = false,
    val t1Visible: Boolean = true,
    val a1Locked: Boolean = false,
    val a1Muted: Boolean = false
)

@Serializable
data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.FHD_1080,
    val fps: ExportFps = ExportFps.FPS_60,
    val dynamicRange: ExportDynamicRange = ExportDynamicRange.SDR_8BIT
)

@Serializable
data class KbEditorProject(
    val projectId: String = UUID.randomUUID().toString(),
    val projectName: String = "Studio Sequence 01",
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val canvasFillMode: CanvasFillMode = CanvasFillMode.FILL_CROP,
    val canvasBlurRadius: Float = 24f,
    val canvasSolidColorHex: Long = 0xFF090D16,
    val safeZoneVisible: Boolean = false,
    val tracks: TrackState = TrackState(),
    val videoClips: List<VideoClip> = defaultSampleVideoClips(),
    val pipClips: List<OverlayPipClip> = emptyList(),
    val captionClips: List<CaptionClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val exportSettings: ExportSettings = ExportSettings()
) {
    val totalDurationMs: Long
        get() {
            val vEnd = videoClips.sumOf { it.activeDurationMs }
            if (videoClips.any { it.mediaUri.isNotBlank() }) {
                return vEnd.coerceAtLeast(1000L)
            }
            val oEnd = pipClips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val tEnd = captionClips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            val aEnd = audioClips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
            return maxOf(vEnd, oEnd, tEnd, aEnd, 1000L)
        }
}

fun defaultSampleVideoClips(): List<VideoClip> = listOf(
    VideoClip(
        title = "A-Roll Intro",
        sourceDurationMs = 5000L,
        trimStartMs = 0L,
        trimEndMs = 5000L,
        transitionOut = TransitionType.CROSS_DISSOLVE
    ),
    VideoClip(
        title = "B-Roll Hero",
        sourceDurationMs = 4500L,
        trimStartMs = 0L,
        trimEndMs = 4500L,
        speedRamp = SpeedRampPreset.HERO_DIP
    )
)
