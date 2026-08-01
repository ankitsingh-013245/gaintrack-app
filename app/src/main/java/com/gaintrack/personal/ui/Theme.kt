package com.gaintrack.personal.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GainOrange = Color(0xFFF47B35)
val GainOrangeDeep = Color(0xFF9A3D0C)
val GainPeach = Color(0xFFFFD9BF)
val GainCream = Color(0xFFFFF4E8)
val GainBackground = Color(0xFFFFF9F2)
val GainText = Color(0xFF2B211C)
val GainGreen = Color(0xFF177A52)
val GainGreenDeep = Color(0xFF146347)
val GainMint = Color(0xFFCFEFDF)
val GainLime = Color(0xFFE8EDB8)
val GainAmber = Color(0xFF955A00)
val GainRose = Color(0xFFD96570)
val GainBlue = Color(0xFF315FAF)
val GainLavender = Color(0xFF6D49A5)

val LocalMotionEnabled = staticCompositionLocalOf { true }

private val LightColors = lightColorScheme(
    primary = GainOrangeDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2CC),
    onPrimaryContainer = GainOrangeDeep,
    secondary = GainBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE8FF),
    onSecondaryContainer = Color(0xFF183665),
    tertiary = GainLavender,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECE1FF),
    onTertiaryContainer = Color(0xFF48266E),
    background = GainBackground,
    onBackground = GainText,
    surface = Color(0xFFFFFCF8),
    onSurface = GainText,
    surfaceVariant = Color(0xFFF4EAE1),
    onSurfaceVariant = Color(0xFF6D5B50),
    outline = Color(0xFF9C887B),
    outlineVariant = Color(0xFFE4D4C8),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val GainTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.35).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    ),
)

private val GainShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun GainTrackTheme(content: @Composable () -> Unit) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    CompositionLocalProvider(LocalMotionEnabled provides motionEnabled) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = GainTypography,
            shapes = GainShapes,
            content = content,
        )
    }
}
