package org.openedx.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

data class WindowSize(
    val width: WindowType,
    val height: WindowType
) {
    val isTablet: Boolean
        get() = height != WindowType.Compact && width != WindowType.Compact
}

fun <T> WindowSize.windowSizeValue(expanded: T, compact: T): T {
    return if (height != WindowType.Compact && width != WindowType.Compact) {
        expanded
    } else {
        compact
    }
}

enum class WindowType {
    Compact, Medium, Expanded
}

@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth by remember(key1 = configuration) {
        mutableStateOf(configuration.screenWidthDp)
    }
    val screenHeight by remember(key1 = configuration) {
        mutableStateOf(configuration.screenHeightDp)
    }

    return WindowSize(
        width = getScreenWidth(screenWidth),
        height = getScreenHeight(screenHeight)
    )
}

fun getScreenWidth(width: Int): WindowType = when {
    width < 600 -> WindowType.Compact
    width < 840 -> WindowType.Medium
    else -> WindowType.Expanded
}

fun getScreenHeight(height: Int): WindowType = when {
    height < 480 -> WindowType.Compact
    height < 900 -> WindowType.Medium
    else -> WindowType.Expanded
}