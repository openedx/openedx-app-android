package org.openedx.core.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

data class AppColors(
    val material: Colors,

    val textPrimary: Color,
    val textPrimaryVariant: Color,
    val textSecondary: Color,
    val textDark: Color,
    val textAccent: Color,

    val textFieldBackground: Color,
    val textFieldBackgroundVariant: Color,
    val textFieldBorder: Color,
    val textFieldText: Color,
    val textFieldHint: Color,

    val buttonBackground: Color,
    val buttonSecondaryBackground: Color,
    val buttonText: Color,

    val cardViewBackground: Color,
    val cardViewBorder: Color,
    val divider: Color,

    val certificateForeground: Color,
    val bottomSheetToggle: Color,
    val warning: Color,
    val info: Color,

    val accessGreen: Color,

    val datesBadgeDefault: Color,
    val datesBadgeTextDefault: Color,
    val datesBadgePastDue: Color,
    val datesBadgeToday: Color,
    val datesBadgeTextToday: Color, // also used for locked date block
    val datesBadgeDue: Color, // Also used for not release date block text and stoke
    val datesBadgeTextDue: Color // Also used for locked date block color
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