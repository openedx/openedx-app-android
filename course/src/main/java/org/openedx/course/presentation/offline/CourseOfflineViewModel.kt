package org.openedx.course.presentation.offline

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.toFileSize
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.course.domain.interactor.CourseInteractor

class CourseOfflineViewModel(
    val courseId: String,
    val courseTitle: String,
    val courseInteractor: CourseInteractor,
    private val preferencesManager: CorePreferences,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
    downloadHelper: DownloadHelper,
) : BaseDownloadViewModel(
    courseId,
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics,
    downloadHelper,
) {
    private val _uiState = MutableStateFlow(
        CourseOfflineUIState(
            isHaveDownloadableBlocks = false,
            readyToDownloadSize = "",
            downloadedSize = "",
            progressBarValue = 0f
        )
    )
    val uiState: StateFlow<CourseOfflineUIState>
        get() = _uiState.asStateFlow()

    init {
        getOfflineData()
    }

    private fun getOfflineData() {
        viewModelScope.launch {
            val courseStructure = courseInteractor.getCourseStructure(courseId)
            val downloadableFilesSize = getFilesSize(courseStructure.blockData)
            if (downloadableFilesSize == 0L) return@launch

            courseInteractor.getDownloadModels().collect { downloadModels ->
                val downloadedModelsIds = downloadModels
                    .filter { it.downloadedState.isDownloaded && it.courseId == courseId }
                    .map { it.id }
                val downloadedBlocks = courseStructure.blockData.filter { it.id in downloadedModelsIds }
                val downloadedFilesSize = getFilesSize(downloadedBlocks)

                _uiState.update {
                    it.copy(
                        isHaveDownloadableBlocks = true,
                        readyToDownloadSize = (downloadableFilesSize - downloadedFilesSize).toFileSize(0, false),
                        downloadedSize = downloadedFilesSize.toFileSize(0, false),
                        progressBarValue = downloadedFilesSize.toFloat() / downloadableFilesSize.toFloat()
                    )
                }
            }
        }
    }

    private fun getFilesSize(block: List<Block>): Long {
        return block.filter { it.isDownloadable }.sumOf {
            when (it.downloadableType) {
                FileType.VIDEO -> {
                    val videoInfo =
                        it.studentViewData?.encodedVideos?.getPreferredVideoInfoForDownloading(
                            preferencesManager.videoSettings.videoDownloadQuality
                        )
                    videoInfo?.fileSize ?: 0
                }

                FileType.X_BLOCK -> {
                    it.offlineDownload?.fileSize ?: 0
                }

                null -> 0
            }
        }
    }
}
