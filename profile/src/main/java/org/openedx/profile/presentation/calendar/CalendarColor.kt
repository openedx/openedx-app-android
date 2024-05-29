package org.openedx.profile.presentation.calendar

import androidx.annotation.StringRes
import org.openedx.profile.R

enum class CalendarColor(
    @StringRes
    val title: Int,
    val color: Long
) {
    ACCENT(R.string.calendar_color_accent, 0xFFD13329),
    RED(R.string.calendar_color_red, 0xFFFF2967),
    ORANGE(R.string.calendar_color_orange, 0xFFFF9501),
    YELLOW(R.string.calendar_color_yellow, 0xFFFFCC01),
    GREEN(R.string.calendar_color_green, 0xFF64DA38),
    BLUE(R.string.calendar_color_blue, 0xFF1AAEF8),
    PURPLE(R.string.calendar_color_purple, 0xFFCC73E1),
    BROWN(R.string.calendar_color_brown, 0xFFA2845E);
}
