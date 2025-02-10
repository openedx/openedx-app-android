package org.openedx.core.presentation.settings.calendarsync

data class DialogProperties(
    val title: String,
    val message: String,
    val positiveButton: String,
    val negativeButton: String,
    val positiveAction: () -> Unit,
    val negativeAction: () -> Unit = {},
)
