package org.openedx.core.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.openedx.core.R

data class AppTypography(
    val defaultFontFamily: FontFamily,
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineBold: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

val fontFamily = FontFamily(
    Font(R.font.regular, FontWeight.Black, FontStyle.Normal),
    Font(R.font.bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.extra_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.semi_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.thin, FontWeight.Thin, FontStyle.Normal),
)

internal val LocalTypography = staticCompositionLocalOf {
    AppTypography(
        displayLarge = TextStyle(
            fontSize = 57.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.25).sp,
            fontFamily = fontFamily
        ),
        displayMedium = TextStyle(
            fontSize = 45.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        displaySmall = TextStyle(
            fontSize = 36.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        headlineLarge = TextStyle(
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        headlineBold = TextStyle(
            fontSize = 34.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        headlineMedium = TextStyle(
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        headlineSmall = TextStyle(
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        titleLarge = TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        titleMedium = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
            fontFamily = fontFamily
        ),
        titleSmall = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            fontFamily = fontFamily
        ),
        bodyLarge = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp,
            fontFamily = fontFamily
        ),
        bodyMedium = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp,
            fontFamily = fontFamily
        ),
        bodySmall = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            fontFamily = fontFamily
        ),
        labelLarge = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            fontFamily = fontFamily
        ),
        labelMedium = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp,
            fontFamily = fontFamily
        ),
        labelSmall = TextStyle(
            fontSize = 10.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            fontFamily = fontFamily
        ),
        defaultFontFamily = fontFamily
    )
}

val MaterialTheme.appTypography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTypography.current
