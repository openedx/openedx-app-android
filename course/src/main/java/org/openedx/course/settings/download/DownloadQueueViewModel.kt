package org.openedx.course.settings.download

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.DownloadProgressChanged

class DownloadQueueViewModel(
    private val descendants: List<String>,
    downloadDao: DownloadDao,
    preferencesManager: CorePreferences,
    private val workerController: DownloadWorkerController,
    private val downloadNotifier: DownloadNotifier,
    coreAnalytics: CoreAnalytics,
    downloadHelper: DownloadHelper,
) : BaseDownloadViewModel(
    "",
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics,
    downloadHelper
) {

    private val _uiState = MutableStateFlow<DownloadQueueUIState>(DownloadQueueUIState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadingModelsFlow.collect { models ->
                val filteredModels =
                    if (descendants.isEmpty()) models else models.filter { descendants.contains(it.id) }
                if (filteredModels.isEmpty()) {
                    _uiState.value = DownloadQueueUIState.Empty
                } else {
                    if (_uiState.value is DownloadQueueUIState.Models) {
                        val state = _uiState.value as DownloadQueueUIState.Models
                        _uiState.value = state.copy(
                            downloadingModels = filteredModels
                        )
                    } else {
                        _uiState.value = DownloadQueueUIState.Models(
                            downloadingModels = filteredModels,
                            currentProgressId = "",
                            currentProgressValue = 0L,
                            currentProgressSize = 0L
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadNotifier.notifier.collect { event ->
                if (event is DownloadProgressChanged) {
                    if (_uiState.value is DownloadQueueUIState.Models) {
                        val state = _uiState.value as DownloadQueueUIState.Models
                        _uiState.value = state.copy(
                            currentProgressId = event.id,
                            currentProgressValue = event.value,
                            currentProgressSize = event.size
                        )
                    }
                }
            }
        }
    }

    override fun removeDownloadModels(blockId: String) {
        viewModelScope.launch {
            workerController.removeModel(blockId)
        }
    }
}
