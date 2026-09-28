package com.novatv.plus.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// NOVA TV+ Minimal Monochrome Palette
// Pure black / Graphite / Subtle Silver
// ==========================================
val BackgroundBlack = Color(0xFF050505)
val SurfaceGraphite0D = Color(0xFF0D0D0D)
val SurfaceGraphite14 = Color(0xFF141414)
val SurfaceGraphite1E = Color(0xFF1E1E1E)
val SurfaceGraphite28 = Color(0xFF282828)

val BorderSubtle = Color(0xFF242424)
val BorderFocused = Color(0xFFFFFFFF)
val BorderSubtleHighlight = Color(0xFF3E3E3E)

val TextPrimaryWhite = Color(0xFFF5F5F7)
val TextSecondarySilver = Color(0xFFA1A1A6)
val TextMutedGraphite = Color(0xFF6E6E73)

val AccentSilver = Color(0xFFE5E5EA)
val AccentSilverDim = Color(0xFF8E8E93)
val LiveIndicatorRed = Color(0xFFE50914) // Subtle cinematic red for live broadcast badge

private val NovaMonochromeColorScheme: ColorScheme = darkColorScheme(
    primary = TextPrimaryWhite,
    onPrimary = BackgroundBlack,
    primaryContainer = SurfaceGraphite1E,
    onPrimaryContainer = TextPrimaryWhite,
    secondary = AccentSilver,
    onSecondary = BackgroundBlack,
    background = BackgroundBlack,
    onBackground = TextPrimaryWhite,
    surface = SurfaceGraphite0D,
    onSurface = TextPrimaryWhite,
    surfaceVariant = SurfaceGraphite14,
    onSurfaceVariant = TextSecondarySilver,
    outline = BorderSubtle,
    outlineVariant = BorderSubtleHighlight,
    error = LiveIndicatorRed,
    onError = TextPrimaryWhite
)

val NovaTvShapes = Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp)
)

val NovaTvTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 34.sp,
        color = TextPrimaryWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 28.sp,
        color = TextPrimaryWhite
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.2.sp,
        lineHeight = 22.sp,
        color = TextPrimaryWhite
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 20.sp,
        color = TextPrimaryWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
        color = TextPrimaryWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 18.sp,
        color = TextSecondarySilver
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        color = TextPrimaryWhite
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        color = TextSecondarySilver
    )
)

@Composable
fun NovaTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaMonochromeColorScheme,
        shapes = NovaTvShapes,
        typography = NovaTvTypography,
        content = content
    )
}
