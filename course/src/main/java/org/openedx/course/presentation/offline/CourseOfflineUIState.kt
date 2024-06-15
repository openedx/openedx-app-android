package org.openedx.course.presentation.offline

data class CourseOfflineUIState(
    val isHaveDownloadableBlocks: Boolean,
    val isDownloading: Boolean,
    val readyToDownloadSize: String,
    val downloadedSize: String,
    val progressBarValue: Float
)
