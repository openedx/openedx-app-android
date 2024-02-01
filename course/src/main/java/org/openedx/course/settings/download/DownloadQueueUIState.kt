package org.openedx.course.settings.download

import org.openedx.core.module.db.DownloadModel

sealed class DownloadQueueUIState {
    data class Models(
        val downloadingModels: List<DownloadModel>
    ) : DownloadQueueUIState()

    object Loading : DownloadQueueUIState()

    object Empty : DownloadQueueUIState()
}
