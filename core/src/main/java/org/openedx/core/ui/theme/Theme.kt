package org.openedx.core.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val DarkColorPalette = AppColors(
    material = darkColors(
        primary = dark_primary,
        primaryVariant = dark_primary_variant,
        secondary = dark_secondary,
        secondaryVariant = dark_secondary_variant,
        background = dark_background,
        surface = dark_surface,
        error = dark_error,
        onPrimary = dark_onPrimary,
        onSecondary = dark_onSecondary,
        onBackground = dark_onBackground,
        onSurface = dark_onSurface,
        onError = dark_onError
    ),
    textPrimary = dark_text_primary,
    textPrimaryVariant = dark_text_primary_variant,
    textPrimaryLight = dark_text_primary_light,
    textSecondary = dark_text_secondary,
    textDark = dark_text_dark,
    textAccent = dark_text_accent,
    textWarning = dark_text_warning,
    textHyperLink = dark_text_hyper_link,

    textFieldBackground = dark_text_field_background,
    textFieldBackgroundVariant = dark_text_field_background_variant,
    textFieldBorder = dark_text_field_border,
    textFieldText = dark_text_field_text,
    textFieldHint = dark_text_field_hint,

    primaryButtonBackground = dark_primary_button_background,
    primaryButtonText = dark_primary_button_text,
    primaryButtonBorder = dark_primary_button_border,
    primaryButtonBorderedText = dark_primary_button_bordered_text,

    secondaryButtonBackground = dark_secondary_button_background,
    secondaryButtonText = dark_secondary_button_text,
    secondaryButtonBorder = dark_secondary_button_border,
    secondaryButtonBorderedBackground = dark_secondary_button_bordered_background,
    secondaryButtonBorderedText = dark_secondary_button_bordered_text,

    cardViewBackground = dark_card_view_background,
    cardViewBorder = dark_card_view_border,
    divider = dark_divider,

    certificateForeground = dark_certificate_foreground,
    bottomSheetToggle = dark_bottom_sheet_toggle,

    warning = dark_warning,
    info = dark_info,
    infoVariant = dark_info_variant,
    onWarning = dark_onWarning,
    onInfo = dark_onInfo,

    rateStars = dark_rate_stars,
    inactiveButtonBackground = dark_inactive_button_background,
    inactiveButtonText = dark_primary_button_text,

    successGreen = dark_success_green,
    successBackground = dark_success_background,

    datesSectionBarPastDue = dark_dates_section_bar_past_due,
    datesSectionBarToday = dark_dates_section_bar_today,
    datesSectionBarThisWeek = dark_dates_section_bar_this_week,
    datesSectionBarNextWeek = dark_dates_section_bar_next_week,
    datesSectionBarUpcoming = dark_dates_section_bar_upcoming,

    authSSOSuccessBackground = dark_auth_sso_success_background,
    authGoogleButtonBackground = dark_auth_google_button_background,
    authFacebookButtonBackground = dark_auth_facebook_button_background,
    authMicrosoftButtonBackground = dark_auth_microsoft_button_background,

    componentHorizontalProgressCompletedAndSelected = dark_component_horizontal_progress_completed_and_selected,
    componentHorizontalProgressCompleted = dark_component_horizontal_progress_completed,
    componentHorizontalProgressSelected = dark_component_horizontal_progress_selected,
    componentHorizontalProgressDefault = dark_component_horizontal_progress_default,

    tabUnselectedBtnBackground = dark_tab_unselected_btn_background,
    tabUnselectedBtnContent = dark_tab_unselected_btn_content,
    tabSelectedBtnContent = dark_tab_selected_btn_content,
    courseHomeHeaderShade = dark_course_home_header_shade,
    courseHomeBackBtnBackground = dark_course_home_back_btn_background,

    settingsTitleContent = dark_settings_title_content,

    progressBarColor = dark_progress_bar_color,
    progressBarBackgroundColor = dark_progress_bar_background_color,
    gradeProgressBarBorder = dark_grade_progress_bar_color,
    gradeProgressBarBackground = dark_grade_progress_bar_background,
    assignmentCardBorder = dark_assignment_card_border,
)

private val LightColorPalette = AppColors(
    material = lightColors(
        primary = light_primary,
        primaryVariant = light_primary_variant,
        secondary = light_secondary,
        secondaryVariant = light_secondary_variant,
        background = light_background,
        surface = light_surface,
        error = light_error,
        onPrimary = light_onPrimary,
        onSecondary = light_onSecondary,
        onBackground = light_onBackground,
        onSurface = light_onSurface,
        onError = light_onError
    ),
    textPrimary = light_text_primary,
    textPrimaryVariant = light_text_primary_variant,
    textPrimaryLight = light_text_primary_light,
    textSecondary = light_text_secondary,
    textDark = light_text_dark,
    textAccent = light_text_accent,
    textWarning = light_text_warning,
    textHyperLink = light_text_hyper_link,

    textFieldBackground = light_text_field_background,
    textFieldBackgroundVariant = light_text_field_background_variant,
    textFieldBorder = light_text_field_border,
    textFieldText = light_text_field_text,
    textFieldHint = light_text_field_hint,

    primaryButtonBackground = light_primary_button_background,
    primaryButtonText = light_primary_button_text,
    primaryButtonBorder = light_primary_button_border,
    primaryButtonBorderedText = light_primary_button_bordered_text,

    secondaryButtonBackground = light_secondary_button_background,
    secondaryButtonText = light_secondary_button_text,
    secondaryButtonBorder = light_secondary_button_border,
    secondaryButtonBorderedBackground = light_secondary_button_bordered_background,
    secondaryButtonBorderedText = light_secondary_button_bordered_text,

    cardViewBackground = light_card_view_background,
    cardViewBorder = light_card_view_border,
    divider = light_divider,

    certificateForeground = light_certificate_foreground,
    bottomSheetToggle = light_bottom_sheet_toggle,

    warning = light_warning,
    info = light_info,
    infoVariant = light_info_variant,
    onWarning = light_onWarning,
    onInfo = light_onInfo,

    rateStars = light_rate_stars,
    inactiveButtonBackground = light_inactive_button_background,
    inactiveButtonText = light_primary_button_text,

    successGreen = light_success_green,
    successBackground = light_success_background,

    datesSectionBarPastDue = light_dates_section_bar_past_due,
    datesSectionBarToday = light_dates_section_bar_today,
    datesSectionBarThisWeek = light_dates_section_bar_this_week,
    datesSectionBarNextWeek = light_dates_section_bar_next_week,
    datesSectionBarUpcoming = light_dates_section_bar_upcoming,

    authSSOSuccessBackground = light_auth_sso_success_background,
    authGoogleButtonBackground = light_auth_google_button_background,
    authFacebookButtonBackground = light_auth_facebook_button_background,
    authMicrosoftButtonBackground = light_auth_microsoft_button_background,

    componentHorizontalProgressCompletedAndSelected = light_component_horizontal_progress_completed_and_selected,
    componentHorizontalProgressCompleted = light_component_horizontal_progress_completed,
    componentHorizontalProgressSelected = light_component_horizontal_progress_selected,
    componentHorizontalProgressDefault = light_component_horizontal_progress_default,

    tabUnselectedBtnBackground = light_tab_unselected_btn_background,
    tabUnselectedBtnContent = light_tab_unselected_btn_content,
    tabSelectedBtnContent = light_tab_selected_btn_content,
    courseHomeHeaderShade = light_course_home_header_shade,
    courseHomeBackBtnBackground = light_course_home_back_btn_background,

    settingsTitleContent = light_settings_title_content,

    progressBarColor = light_progress_bar_color,
    progressBarBackgroundColor = light_progress_bar_background_color,
    gradeProgressBarBorder = light_grade_progress_bar_color,
    gradeProgressBarBackground = light_grade_progress_bar_background,
    assignmentCardBorder = light_assignment_card_border,
)

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = if (colors.isLight) LightColorPalette else DarkColorPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenEdXTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors.material,
        // typography = LocalTypography.current.material,
        shapes = LocalShapes.current.material,
    ) {
        CompositionLocalProvider(
            LocalOverscrollFactory provides null,
            content = content
        )
    }
}
