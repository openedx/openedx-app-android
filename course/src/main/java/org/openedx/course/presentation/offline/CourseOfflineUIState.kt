package org.openedx.course.presentation.offline

import org.openedx.core.module.db.DownloadModel

data class CourseOfflineUIState(
    val isHaveDownloadableBlocks: Boolean,
    val isAllDownloaded: Boolean,
    val largestDownloads: List<DownloadModel>,
    val isDownloading: Boolean,
    val readyToDownloadSize: Long,
    val downloadedSize: Long,
    val progressBarValue: Float,
)
