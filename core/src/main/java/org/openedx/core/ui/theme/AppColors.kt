package org.openedx.core.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

data class AppColors(
    val material: Colors,

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
    val primary: Color get() = material.primary
    val primaryVariant: Color get() = material.primaryVariant
    val secondary: Color get() = material.secondary
    val secondaryVariant: Color get() = material.secondaryVariant
    val background: Color get() = material.background
    val surface: Color get() = material.surface
    val error: Color get() = material.error
    val onPrimary: Color get() = material.onPrimary
    val onSecondary: Color get() = material.onSecondary
    val onBackground: Color get() = material.onBackground
    val onSurface: Color get() = material.onSurface
    val onError: Color get() = material.onError
    val isLight: Boolean get() = material.isLight
}
