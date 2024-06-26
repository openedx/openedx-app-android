package org.openedx.course.domain.model

import androidx.compose.ui.graphics.painter.Painter

data class DownloadDialogResource(
    val title: String,
    val description: String,
    val icon: Painter? = null,
)
