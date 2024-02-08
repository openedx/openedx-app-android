package org.openedx.course.settings.download

import org.openedx.core.module.db.DownloadModel

sealed class DownloadQueueUIState {
    data class Models(
        val downloadingModels: List<DownloadModel>,
        val currentProgressId: String,
        val currentProgressValue: Long,
        val currentProgressSize: Long
    ) : DownloadQueueUIState()

    object Loading : DownloadQueueUIState()

    object Empty : DownloadQueueUIState()
}
