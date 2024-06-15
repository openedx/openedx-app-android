package org.openedx.course.presentation.offline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.toFileSize
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.utils.FileUtil
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.download.DownloadDialogItem
import org.openedx.course.presentation.download.DownloadDialogManager

class CourseOfflineViewModel(
    val courseId: String,
    val courseTitle: String,
    val courseInteractor: CourseInteractor,
    private val preferencesManager: CorePreferences,
    private val downloadDialogManager: DownloadDialogManager,
    private val fileUtil: FileUtil,
    private val courseRouter: CourseRouter,
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
            largestDownloads = emptyList(),
            isDownloading = false,
            readyToDownloadSize = "",
            downloadedSize = "",
            progressBarValue = 0f
        )
    )
    val uiState: StateFlow<CourseOfflineUIState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                val isDownloading = it.any { it.value.isWaitingOrDownloading }
                _uiState.update { it.copy(isDownloading = isDownloading) }
            }
        }

        viewModelScope.launch {
            async { initDownloadFragment() }.await()
            getOfflineData()
        }
    }

    fun downloadAllBlocks(fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val courseStructure = courseInteractor.getCourseStructureFromCache(courseId)
            val downloadModels = courseInteractor.getAllDownloadModels()
            val subSectionsBlocks = allBlocks.values.filter { it.type == BlockType.SEQUENTIAL }
            val notDownloadedSubSectionBlocks = subSectionsBlocks.mapNotNull { subSectionsBlock ->
                val verticalBlocks = allBlocks.values.filter { it.id in subSectionsBlock.descendants }
                val notDownloadedBlocks = courseStructure.blockData.filter { block ->
                    block.id in verticalBlocks.flatMap { it.descendants } && block.isDownloadable && !downloadModels.any { it.id == block.id }
                }
                if (notDownloadedBlocks.isNotEmpty()) subSectionsBlock else null
            }

            downloadDialogManager.showPopup(
                subSectionsBlocks = notDownloadedSubSectionBlocks,
                courseId = courseId,
                isAllBlocksDownloaded = false,
                fragmentManager = fragmentManager,
                removeDownloadModels = ::removeDownloadModels,
                saveDownloadModels = { blockId ->
                    saveDownloadModels(fileUtil.getExternalAppDir().path, blockId)
                }
            )
        }
    }

    fun navigateToDownloadQueue(fragmentManager: FragmentManager) {
        val downloadableChildren =
            allBlocks.values
                .filter { it.type == BlockType.SEQUENTIAL }
                .mapNotNull { getDownloadableChildren(it.id) }
                .flatten()
        courseRouter.navigateToDownloadQueue(fragmentManager, downloadableChildren)
    }

    fun removeDownloadModel(downloadModel: DownloadModel, fragmentManager: FragmentManager) {
        val icon = if (downloadModel.type == FileType.VIDEO) {
            Icons.Outlined.SmartDisplay
        } else {
            Icons.AutoMirrored.Outlined.InsertDriveFile
        }
        val downloadDialogItem = DownloadDialogItem(
            title = downloadModel.title,
            size = downloadModel.size,
            icon = icon
        )
        downloadDialogManager.showRemoveDownloadModelPopup(
            downloadDialogItem = downloadDialogItem,
            fragmentManager = fragmentManager,
            removeDownloadModels = {
                super.removeBlockDownloadModel(downloadModel.id)
            },
        )
    }

    fun removeDownloadModel(fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val courseStructure = courseInteractor.getCourseStructureFromCache(courseId)
            val downloadModels = courseInteractor.getAllDownloadModels()
            val subSectionsBlocks = allBlocks.values.filter { it.type == BlockType.SEQUENTIAL }
            val downloadedSubSectionBlocks = subSectionsBlocks.mapNotNull { subSectionsBlock ->
                val verticalBlocks = allBlocks.values.filter { it.id in subSectionsBlock.descendants }
                val notDownloadedBlocks = courseStructure.blockData.filter { block ->
                    block.id in verticalBlocks.flatMap { it.descendants } && block.isDownloadable && downloadModels.any { it.id == block.id }
                }
                if (notDownloadedBlocks.isNotEmpty()) subSectionsBlock else null
            }
            downloadDialogManager.showPopup(
                subSectionsBlocks = downloadedSubSectionBlocks,
                courseId = courseId,
                isAllBlocksDownloaded = true,
                fragmentManager = fragmentManager,
                removeDownloadModels = ::removeDownloadModels,
                saveDownloadModels = { blockId ->
                    super.removeDownloadModels(blockId)
                }
            )
        }
    }

    private suspend fun initDownloadFragment() {
        val courseStructure = courseInteractor.getCourseStructureFromCache(courseId)
        setBlocks(courseStructure.blockData)
        allBlocks.values
            .filter { it.type == BlockType.SEQUENTIAL }
            .forEach {
                addDownloadableChildrenForSequentialBlock(it)
            }

    }

    private fun getOfflineData() {
        viewModelScope.launch {
            val courseStructure = courseInteractor.getCourseStructureFromCache(courseId)
            val downloadableFilesSize = getFilesSize(courseStructure.blockData) * 2
            if (downloadableFilesSize == 0L) return@launch

            courseInteractor.getDownloadModels().collect {
                val downloadModels = it.filter { it.downloadedState.isDownloaded && it.courseId == courseId }
                val downloadedModelsIds = downloadModels.map { it.id }
                val downloadedBlocks = courseStructure.blockData.filter { it.id in downloadedModelsIds }
                val downloadedFilesSize = getFilesSize(downloadedBlocks) * 2
                val largestDownloads = downloadModels
                    .sortedByDescending { it.size }
                    .take(5)

                _uiState.update {
                    it.copy(
                        isHaveDownloadableBlocks = true,
                        largestDownloads = largestDownloads,
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
