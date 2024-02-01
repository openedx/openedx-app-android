package org.openedx.course.settings.download

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel

class DownloadQueueViewModel(
    downloadDao: DownloadDao,
    preferencesManager: CorePreferences,
    private val workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    private val _uiState = MutableStateFlow<DownloadQueueUIState>(DownloadQueueUIState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadingModelsFlow.collect { models ->
                if (models.isEmpty()) {
                    _uiState.value = DownloadQueueUIState.Empty

                } else {
                    _uiState.value = DownloadQueueUIState.Models(
                        downloadingModels = models
                    )
                }
            }
        }
    }

    override fun cancelWork(blockId: String) {
        viewModelScope.launch {
            workerController.cancelWork(*arrayOf(blockId))
        }
    }
}
