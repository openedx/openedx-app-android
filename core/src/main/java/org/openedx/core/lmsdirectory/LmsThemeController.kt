package org.openedx.core.lmsdirectory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Holds the accent color the app re-themes to when a learner picks an LMS from the
 * directory. [OpenEdXTheme][org.openedx.core.ui.theme.OpenEdXTheme] reads [accentColor]
 * and, when present, tints the palette's accent-driven surfaces (buttons, primary).
 *
 * Backed by [mutableStateOf] so setting it recomposes the theme. The app seeds it at
 * launch from the persisted selection and updates it the moment a platform is chosen.
 */
object LmsThemeController {

    var accentColor by mutableStateOf<Color?>(null)
        private set

    /** Apply a hex color like "#f15d49". Invalid or blank input clears the override. */
    fun apply(hex: String?) {
        accentColor = parseHexColor(hex)
    }

    fun clear() {
        accentColor = null
    }

    @Suppress("MagicNumber", "ReturnCount")
    fun parseHexColor(hex: String?): Color? {
        val raw = hex?.trim()?.removePrefix("#") ?: return null
        if (raw.length != 6 && raw.length != 8) return null
        val value = raw.toLongOrNull(16) ?: return null
        return when (raw.length) {
            6 -> Color(0xFF000000 or value)
            else -> Color(value)
        }
    }
}
