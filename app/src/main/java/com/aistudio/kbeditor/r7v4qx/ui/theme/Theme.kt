package com.aistudio.kbeditor.r7v4qx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.aistudio.kbeditor.r7v4qx.R

val SlateVoid = Color(0xFF090D16)
val SlateBase = Color(0xFF0F172A)
val SlateElevated = Color(0xFF1E293B)
val SlateBorder = Color(0xFF334155)
val ElectricCyan = Color(0xFF22D3EE)
val WarmAmber = Color(0xFFF59E0B)
val EmeraldTrack = Color(0xFF34D399)
val CoralAmber = Color(0xFFFB7185)
val VioletClip = Color(0xFFA78BFA)
val TextPrimary = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val SpaceGroteskFamily = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = provider, weight = FontWeight.Bold)
)

val PlusJakartaSansFamily = FontFamily(
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.SemiBold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Medium)
)

private val KbColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = SlateVoid,
    secondary = WarmAmber,
    onSecondary = SlateVoid,
    tertiary = VioletClip,
    background = SlateVoid,
    onBackground = TextPrimary,
    surface = SlateBase,
    onSurface = TextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextMuted,
    outline = SlateBorder,
    error = CoralAmber
)

private val KbTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

@Composable
fun KbEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KbColorScheme,
        typography = KbTypography,
        content = content
    )
}
