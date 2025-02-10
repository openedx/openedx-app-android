package org.openedx.core.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

interface TabItem {
    @get:StringRes
    val labelResId: Int
    val icon: ImageVector?
}
