package org.openedx.core.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
    textSecondary = dark_text_secondary,
    textDark = dark_text_dark,
    textAccent = dark_text_accent,

    textFieldBackground = dark_text_field_background,
    textFieldBackgroundVariant = dark_text_field_background_variant,
    textFieldBorder = dark_text_field_border,
    textFieldText = dark_text_field_text,
    textFieldHint = dark_text_field_hint,

    buttonBackground = dark_button_background,
    buttonSecondaryBackground = dark_button_secondary_background,
    buttonText = dark_button_text,

    cardViewBackground = dark_card_view_background,
    cardViewBorder = dark_card_view_border,
    divider = dark_divider,

    certificateForeground = dark_certificate_foreground,
    bottomSheetToggle = dark_bottom_sheet_toggle,

    warning = dark_warning,
    info = dark_info,

    rateStars = dark_rate_stars,
    inactiveButtonBackground = dark_inactive_button_background,
    inactiveButtonText = dark_button_text,

    accessGreen = dark_access_green,

    datesBadgeDefault = dark_dates_badge_default,
    datesBadgeTextDefault = dark_dates_badge_text_default,
    datesBadgePastDue = dark_dates_badge_past_due,
    datesBadgeToday = dark_dates_badge_today,
    datesBadgeTextToday = dark_dates_badge_text_today,
    datesBadgeDue = dark_dates_badge_due,
    datesBadgeTextDue = dark_dates_badge_text_due,

    authFacebookButtonBackground = dark_auth_facebook_button_background,
    authMicrosoftButtonBackground = dark_auth_microsoft_button_background,

    componentHorizontalProgressCompleted = dark_component_horizontal_progress_completed,
    componentHorizontalProgressSelected = dark_component_horizontal_progress_selected,
    componentHorizontalProgressDefault = dark_component_horizontal_progress_default,
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
    textSecondary = light_text_secondary,
    textDark = light_text_dark,
    textAccent = light_text_accent,

    textFieldBackground = light_text_field_background,
    textFieldBackgroundVariant = light_text_field_background_variant,
    textFieldBorder = light_text_field_border,
    textFieldText = light_text_field_text,
    textFieldHint = light_text_field_hint,

    buttonBackground = light_button_background,
    buttonSecondaryBackground = light_button_secondary_background,
    buttonText = light_button_text,

    cardViewBackground = light_card_view_background,
    cardViewBorder = light_card_view_border,
    divider = light_divider,

    certificateForeground = light_certificate_foreground,
    bottomSheetToggle = light_bottom_sheet_toggle,

    warning = light_warning,
    info = light_info,

    rateStars = light_rate_stars,
    inactiveButtonBackground = light_inactive_button_background,
    inactiveButtonText = light_button_text,

    accessGreen = light_access_green,

    datesBadgeDefault = light_dates_badge_default,
    datesBadgeTextDefault = light_dates_badge_text_default,
    datesBadgePastDue = light_dates_badge_past_due,
    datesBadgeToday = light_dates_badge_today,
    datesBadgeTextToday = light_dates_badge_text_today,
    datesBadgeDue = light_dates_badge_due,
    datesBadgeTextDue = light_dates_badge_text_due,

    authFacebookButtonBackground = light_auth_facebook_button_background,
    authMicrosoftButtonBackground = light_auth_microsoft_button_background,

    componentHorizontalProgressCompleted = light_component_horizontal_progress_completed,
    componentHorizontalProgressSelected = light_component_horizontal_progress_selected,
    componentHorizontalProgressDefault = light_component_horizontal_progress_default,
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
        //typography = LocalTypography.current.material,
        shapes = LocalShapes.current.material,
    ) {
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
            content = content
        )
    }
}
