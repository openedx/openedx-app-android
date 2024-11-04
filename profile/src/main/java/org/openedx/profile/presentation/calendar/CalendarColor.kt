package org.openedx.profile.presentation.calendar

import androidx.annotation.StringRes
import org.openedx.profile.R

private const val ACCENT_COLOR = 0xFFD13329L
private const val RED_COLOR = 0xFFFF2967L
private const val ORANGE_COLOR = 0xFFFF9501L
private const val YELLOW_COLOR = 0xFFFFCC01L
private const val GREEN_COLOR = 0xFF64DA38L
private const val BLUE_COLOR = 0xFF1AAEF8L
private const val PURPLE_COLOR = 0xFFCC73E1L
private const val BROWN_COLOR = 0xFFA2845EL

enum class CalendarColor(
    @StringRes
    val title: Int,
    val color: Long
) {
    ACCENT(R.string.calendar_color_accent, ACCENT_COLOR),
    RED(R.string.calendar_color_red, RED_COLOR),
    ORANGE(R.string.calendar_color_orange, ORANGE_COLOR),
    YELLOW(R.string.calendar_color_yellow, YELLOW_COLOR),
    GREEN(R.string.calendar_color_green, GREEN_COLOR),
    BLUE(R.string.calendar_color_blue, BLUE_COLOR),
    PURPLE(R.string.calendar_color_purple, PURPLE_COLOR),
    BROWN(R.string.calendar_color_brown, BROWN_COLOR)
}
