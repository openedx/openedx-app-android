package org.openedx.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Branding of one instance: an accent color plus optional per-field palette overrides for
 * light and dark mode, and an optional logo and header image.
 *
 * Takes primitives rather than the instance model, so the theme layer stays independent of
 * how instances are configured.
 */
data class InstanceTheme(
    /** Stable instance key. Never a display name, which can be renamed or localized. */
    val key: String,
    val accentColorHex: String? = null,
    val light: Map<String, String> = emptyMap(),
    val dark: Map<String, String> = emptyMap(),
    /** Remote URL, or the name of a bundled drawable. */
    val logoUrl: String? = null,
    /** Remote URL, or the name of a bundled drawable. */
    val headerBackgroundUrl: String? = null,
) {

    /**
     * The default palette with this instance's colors layered on top. Only the fields the
     * instance sets change; everything else keeps the compiled value. A dark override falls
     * back to the light one for the same field. A field set only for dark mode changes dark
     * mode alone: light mode keeps the compiled value.
     */
    fun applyTo(base: AppColors, isDark: Boolean): AppColors {
        val modeAccent = if (isDark) dark[ACCENT_COLOR] ?: light[ACCENT_COLOR] else light[ACCENT_COLOR]
        var colors = (modeAccent ?: accentColorHex)?.toColorOrNull()?.let(base::withAccent) ?: base
        (light.keys + dark.keys).forEach { field ->
            if (field == ACCENT_COLOR) return@forEach
            val hex = (if (isDark) dark[field] ?: light[field] else light[field]) ?: return@forEach
            val color = hex.toColorOrNull() ?: return@forEach
            colors = paletteFields[field]?.invoke(colors, color) ?: colors
        }
        return colors
    }

    companion object {
        const val ACCENT_COLOR = "accent_color"

        /**
         * Palette field names shared with the iOS app, mapped to the colors they override here.
         * Unknown names are ignored rather than treated as errors.
         */
        internal val paletteFields: Map<String, (AppColors, Color) -> AppColors> = mapOf(
            "accent_x_color" to { c, v -> c.copy(material3 = c.material3.copy(secondary = v)) },
            "alert" to { c, v -> c.copy(material3 = c.material3.copy(error = v)) },
            "background" to { c, v -> c.copy(material3 = c.material3.copy(background = v)) },
            "card_view_background" to { c, v -> c.copy(cardViewBackground = v) },
            "card_view_stroke" to { c, v -> c.copy(cardViewBorder = v) },
            "certificate_foreground" to { c, v -> c.copy(certificateForeground = v) },
            "text_primary" to { c, v -> c.copy(textPrimary = v) },
            "text_secondary" to { c, v -> c.copy(textSecondary = v) },
            "text_input_background" to { c, v -> c.copy(textFieldBackground = v) },
            "text_input_stroke" to { c, v -> c.copy(textFieldBorder = v) },
            "text_input_text_color" to { c, v -> c.copy(textFieldText = v) },
            "text_input_placeholder_color" to { c, v -> c.copy(textFieldHint = v) },
            "warning" to { c, v -> c.copy(warning = v) },
            "success" to { c, v -> c.copy(successGreen = v) },
            "info_color" to { c, v -> c.copy(info = v) },
            "primary_button_text_color" to { c, v -> c.copy(primaryButtonText = v) },
            "secondary_button_border_color" to { c, v -> c.copy(secondaryButtonBorder = v) },
            "secondary_button_text_color" to { c, v -> c.copy(secondaryButtonBorderedText = v) },
            "past_due_timeline_color" to { c, v -> c.copy(datesSectionBarPastDue = v) },
            "today_timeline_color" to { c, v -> c.copy(datesSectionBarToday = v) },
            "this_week_timeline_color" to { c, v -> c.copy(datesSectionBarThisWeek = v) },
            "next_week_timeline_color" to { c, v -> c.copy(datesSectionBarNextWeek = v) },
            "upcoming_timeline_color" to { c, v -> c.copy(datesSectionBarUpcoming = v) },
        )
    }
}

/**
 * The accent drives every color the default palette derives from `primary`: links, primary
 * buttons, bordered button text and the unselected tab content.
 */
internal fun AppColors.withAccent(accent: Color): AppColors = copy(
    material3 = material3.copy(primary = accent),
    textAccent = accent,
    textHyperLink = accent,
    infoVariant = accent,
    primaryButtonBackground = accent,
    primaryButtonBorderedText = accent,
    secondaryButtonBackground = accent,
    secondaryButtonBorderedText = accent,
    tabUnselectedBtnContent = accent,
)

/** Parses `#RGB`, `#RRGGBB` or `#AARRGGBB`. */
@Suppress("MagicNumber")
internal fun String.toColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    val argb = when (hex.length) {
        3 -> "FF" + hex.map { "$it$it" }.joinToString("")
        6 -> "FF$hex"
        8 -> hex
        else -> null
    }
    return argb
        ?.takeIf { value -> value.all { Character.digit(it, 16) >= 0 } }
        ?.let { Color(it.toLong(16)) }
}

/**
 * Holds the theme of the selected instance; `null` means the compiled default theme.
 * [OpenEdXTheme] observes it, so applying a theme restyles every screen.
 */
object InstanceThemeManager {

    private val _theme = MutableStateFlow<InstanceTheme?>(null)
    val theme: StateFlow<InstanceTheme?> = _theme.asStateFlow()

    fun applyTheme(theme: InstanceTheme?) {
        _theme.value = theme
    }

    fun reset() = applyTheme(null)
}
