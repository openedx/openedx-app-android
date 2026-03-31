package org.openedx.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class AppColors(
    val material3: ColorScheme,

    val textPrimary: Color,
    val textPrimaryVariant: Color,
    val textPrimaryLight: Color,
    val textHyperLink: Color,
    val textSecondary: Color,
    val textDark: Color,
    val textAccent: Color,
    val textWarning: Color,

    val textFieldBackground: Color,
    val textFieldBackgroundVariant: Color,
    val textFieldBorder: Color,
    val textFieldText: Color,
    val textFieldHint: Color,

    val primaryButtonBackground: Color,
    val primaryButtonText: Color,
    val primaryButtonBorder: Color,
    val primaryButtonBorderedText: Color,

    // The default secondary button styling is identical to the primary button styling.
    // However, you can customize it if your brand utilizes two accent colors.
    val secondaryButtonBackground: Color,
    val secondaryButtonText: Color,
    val secondaryButtonBorder: Color,
    val secondaryButtonBorderedBackground: Color,
    val secondaryButtonBorderedText: Color,

    val cardViewBackground: Color,
    val cardViewBorder: Color,
    val divider: Color,

    val certificateForeground: Color,
    val bottomSheetToggle: Color,
    val warning: Color,
    val info: Color,
    val infoVariant: Color,
    val onWarning: Color,
    val onInfo: Color,

    val rateStars: Color,
    val inactiveButtonBackground: Color,
    val inactiveButtonText: Color,

    val successGreen: Color,
    val successBackground: Color,

    val datesSectionBarPastDue: Color,
    val datesSectionBarToday: Color,
    val datesSectionBarThisWeek: Color,
    val datesSectionBarNextWeek: Color,
    val datesSectionBarUpcoming: Color,

    val authSSOSuccessBackground: Color,
    val authGoogleButtonBackground: Color,
    val authFacebookButtonBackground: Color,
    val authMicrosoftButtonBackground: Color,

    val componentHorizontalProgressCompletedAndSelected: Color,
    val componentHorizontalProgressCompleted: Color,
    val componentHorizontalProgressSelected: Color,
    val componentHorizontalProgressDefault: Color,

    val tabUnselectedBtnBackground: Color,
    val tabUnselectedBtnContent: Color,
    val tabSelectedBtnContent: Color,
    val courseHomeHeaderShade: Color,
    val courseHomeBackBtnBackground: Color,

    val settingsTitleContent: Color,

    val progressBarColor: Color,
    val progressBarBackgroundColor: Color,
    val gradeProgressBarBorder: Color,
    val gradeProgressBarBackground: Color,
    val assignmentCardBorder: Color,
) {
    // Material 3 ColorScheme accessors
    val primary: Color get() = material3.primary
    val onPrimary: Color get() = material3.onPrimary
    val primaryContainer: Color get() = material3.primaryContainer
    val onPrimaryContainer: Color get() = material3.onPrimaryContainer
    val secondary: Color get() = material3.secondary
    val onSecondary: Color get() = material3.onSecondary
    val secondaryContainer: Color get() = material3.secondaryContainer
    val onSecondaryContainer: Color get() = material3.onSecondaryContainer
    val tertiary: Color get() = material3.tertiary
    val onTertiary: Color get() = material3.onTertiary
    val tertiaryContainer: Color get() = material3.tertiaryContainer
    val onTertiaryContainer: Color get() = material3.onTertiaryContainer
    val background: Color get() = material3.background
    val onBackground: Color get() = material3.onBackground
    val surface: Color get() = material3.surface
    val onSurface: Color get() = material3.onSurface
    val surfaceVariant: Color get() = material3.surfaceVariant
    val onSurfaceVariant: Color get() = material3.onSurfaceVariant
    val error: Color get() = material3.error
    val onError: Color get() = material3.onError
    val errorContainer: Color get() = material3.errorContainer
    val onErrorContainer: Color get() = material3.onErrorContainer
    val outline: Color get() = material3.outline
    val outlineVariant: Color get() = material3.outlineVariant
    val inverseSurface: Color get() = material3.inverseSurface
    val inverseOnSurface: Color get() = material3.inverseOnSurface
    val inversePrimary: Color get() = material3.inversePrimary
    val surfaceTint: Color get() = material3.surfaceTint
    val scrim: Color get() = material3.scrim

    // Backward compatibility accessors for M1 color names
    @Deprecated("Use primary instead", ReplaceWith("primary"))
    val primaryVariant: Color get() = material3.primaryContainer

    @Deprecated("Use secondary instead", ReplaceWith("secondary"))
    val secondaryVariant: Color get() = material3.secondaryContainer

    // Helper to determine if this is a light theme
    val isLight: Boolean get() = material3.background.luminance() > 0.5f

    private fun Color.luminance(): Float {
        val r = red
        val g = green
        val b = blue
        @Suppress("MagicNumber")
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
