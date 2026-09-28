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

// Color Palette for Nova TV+
val CyanAccent = Color(0xFF00F0FF)
val CyanAccentDark = Color(0xFF009DA8)
val CyanAccentGlow = Color(0x3300F0FF)
val OledBlack = Color(0xFF000000)
val SurfaceDark08 = Color(0xFF080808)
val SurfaceDark11 = Color(0xFF111111)
val SurfaceDark1A = Color(0xFF181818)
val SurfaceBorderDark = Color(0xFF262626)
val LiveRed = Color(0xFFFF2020)
val LiveRedDark = Color(0xFF990000)
val TextWhite = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A0)
val TextMuted = Color(0xFF6E6E6E)

private val NovaTvColorScheme: ColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = OledBlack,
    primaryContainer = Color(0xFF00363B),
    onPrimaryContainer = CyanAccent,
    secondary = Color(0xFF00A0AB),
    onSecondary = TextWhite,
    background = OledBlack,
    onBackground = TextWhite,
    surface = SurfaceDark08,
    onSurface = TextWhite,
    surfaceVariant = SurfaceDark11,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorderDark,
    error = LiveRed,
    onError = TextWhite
)

// Sharp minimal shapes (2.dp to 4.dp)
val NovaTvShapes = Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp)
)

val NovaTvTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = TextWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextWhite
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextWhite
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = TextWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        color = CyanAccent
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        color = TextWhite
    )
)

@Composable
fun NovaTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaTvColorScheme,
        shapes = NovaTvShapes,
        typography = NovaTvTypography,
        content = content
    )
}
