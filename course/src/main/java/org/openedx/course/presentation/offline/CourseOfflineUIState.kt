package org.openedx.course.presentation.offline

import org.openedx.core.module.db.DownloadModel

data class CourseOfflineUIState(
    val isHaveDownloadableBlocks: Boolean,
    val largestDownloads: List<DownloadModel>,
    val isDownloading: Boolean,
    val readyToDownloadSize: String,
    val downloadedSize: String,
    val progressBarValue: Float,
)
