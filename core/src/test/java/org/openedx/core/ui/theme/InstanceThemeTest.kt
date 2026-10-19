package org.openedx.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class InstanceThemeTest {

    private val light = LightColorPalette
    private val dark = DarkColorPalette

    @Test
    fun `no palette derives only the accent colors from the hex`() {
        val colors = InstanceTheme(key = "acme", accentColorHex = "#3E578C").applyTo(light, isDark = false)
        val accent = Color(0xFF3E578C)

        assertEquals(accent, colors.primary)
        assertEquals(accent, colors.textAccent)
        assertEquals(accent, colors.textHyperLink)
        assertEquals(accent, colors.primaryButtonBackground)
        assertEquals(accent, colors.primaryButtonBorderedText)
        assertEquals(light.textPrimary, colors.textPrimary)
        assertEquals(light.background, colors.background)
    }

    @Test
    fun `partial palette overrides only the fields it sets`() {
        val baseline = InstanceTheme(key = "acme", accentColorHex = "#3E578C").applyTo(light, isDark = false)
        val overridden = InstanceTheme(
            key = "acme",
            accentColorHex = "#3E578C",
            light = mapOf("text_primary" to "#18202E"),
        ).applyTo(light, isDark = false)

        assertEquals(Color(0xFF18202E), overridden.textPrimary)
        assertEquals(baseline.primary, overridden.primary)
        assertEquals(baseline.background, overridden.background)
        assertEquals(baseline.cardViewBackground, overridden.cardViewBackground)
    }

    @Test
    fun `palette accent wins over the flat color, per mode`() {
        val theme = InstanceTheme(
            key = "acme",
            accentColorHex = "#007AFF",
            light = mapOf("accent_color" to "#3C68FF"),
            dark = mapOf("accent_color" to "#1C355E"),
        )

        assertEquals(Color(0xFF3C68FF), theme.applyTo(light, isDark = false).primary)
        assertEquals(Color(0xFF1C355E), theme.applyTo(dark, isDark = true).primary)
    }

    @Test
    fun `missing dark value falls back to the light value`() {
        val theme = InstanceTheme(key = "acme", light = mapOf("alert" to "#FF3D71", "accent_color" to "#3C68FF"))

        assertEquals(Color(0xFFFF3D71), theme.applyTo(dark, isDark = true).error)
        assertEquals(Color(0xFF3C68FF), theme.applyTo(dark, isDark = true).primary)
    }

    @Test
    fun `light and dark values resolve independently`() {
        val theme = InstanceTheme(
            key = "acme",
            light = mapOf("background" to "#FFFFFF"),
            dark = mapOf("background" to "#0A0A0A"),
        )

        assertEquals(Color(0xFFFFFFFF), theme.applyTo(light, isDark = false).background)
        assertEquals(Color(0xFF0A0A0A), theme.applyTo(dark, isDark = true).background)
    }

    @Test
    fun `unknown fields and bad hex values are ignored`() {
        val theme = InstanceTheme(
            key = "acme",
            accentColorHex = "not-a-color",
            light = mapOf("not_a_real_field" to "#000000", "text_primary" to "#12"),
        )

        assertEquals(light, theme.applyTo(light, isDark = false))
    }

    @Test
    fun `hex parsing accepts short, long and alpha forms`() {
        assertEquals(Color(0xFFFF0000), "#F00".toColorOrNull())
        assertEquals(Color(0xFF3C68FF), " 3C68FF ".toColorOrNull())
        assertEquals(Color(0x803C68FF), "#803C68FF".toColorOrNull())
        assertNull("#GG0000".toColorOrNull())
        assertNull("-12345".toColorOrNull())
        assertNull("".toColorOrNull())
    }

    @Test
    fun `manager publishes and resets the theme`() {
        val theme = InstanceTheme(key = "acme")

        InstanceThemeManager.applyTheme(theme)
        assertSame(theme, InstanceThemeManager.theme.value)

        InstanceThemeManager.reset()
        assertNull(InstanceThemeManager.theme.value)
    }
}
