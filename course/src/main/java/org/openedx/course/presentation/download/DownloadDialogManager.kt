package org.openedx.course.presentation.download

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.system.StorageManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.domain.interactor.CourseInteractor

class DownloadDialogManager(
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val interactor: CourseInteractor,
    private val workerController: DownloadWorkerController
) {

    companion object {
        const val MAX_CELLULAR_SIZE = 104857600 // 100MB
        const val DOWNLOAD_SIZE_FACTOR = 2 // Multiplier to match required disk size
    }

    private val uiState = MutableSharedFlow<DownloadDialogUIState>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            uiState.collect { uiState ->
                when {
                    uiState.isDownloadFailed -> {
                        val dialog = DownloadErrorDialogFragment.newInstance(
                            dialogType = DownloadErrorDialogType.DOWNLOAD_FAILED,
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadErrorDialogFragment.DIALOG_TAG
                        )
                    }

                    uiState.isAllBlocksDownloaded -> {
                        val dialog = DownloadConfirmDialogFragment.newInstance(
                            dialogType = DownloadConfirmDialogType.REMOVE,
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadConfirmDialogFragment.DIALOG_TAG
                        )
                    }

                    !networkConnection.isOnline() -> {
                        val dialog = DownloadErrorDialogFragment.newInstance(
                            dialogType = DownloadErrorDialogType.NO_CONNECTION,
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadErrorDialogFragment.DIALOG_TAG
                        )
                    }

                    StorageManager.getFreeStorage() < uiState.sizeSum * DOWNLOAD_SIZE_FACTOR -> {
                        val dialog = DownloadStorageErrorDialogFragment.newInstance(
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadStorageErrorDialogFragment.DIALOG_TAG
                        )
                    }

                    corePreferences.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected() -> {
                        val dialog = DownloadErrorDialogFragment.newInstance(
                            dialogType = DownloadErrorDialogType.WIFI_REQUIRED,
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadErrorDialogFragment.DIALOG_TAG
                        )
                    }

                    !corePreferences.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected() -> {
                        val dialog = DownloadConfirmDialogFragment.newInstance(
                            dialogType = DownloadConfirmDialogType.DOWNLOAD_ON_CELLULAR,
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadConfirmDialogFragment.DIALOG_TAG
                        )
                    }

                    uiState.sizeSum >= MAX_CELLULAR_SIZE -> {
                        val dialog = DownloadConfirmDialogFragment.newInstance(
                            dialogType = DownloadConfirmDialogType.CONFIRM,
                            uiState = uiState
                        )
                        dialog.show(
                            uiState.fragmentManager,
                            DownloadConfirmDialogFragment.DIALOG_TAG
                        )
                    }

                    else -> {
                        uiState.saveDownloadModels()
                    }
                }
            }
        }
    }

    fun showPopup(
        subSectionsBlocks: List<Block>,
        courseId: String,
        isBlocksDownloaded: Boolean,
        onlyVideoBlocks: Boolean = false,
        fragmentManager: FragmentManager,
        removeDownloadModels: (blockId: String) -> Unit,
        saveDownloadModels: (blockId: String) -> Unit,
    ) {
        createDownloadItems(
            subSectionsBlocks = subSectionsBlocks,
            courseId = courseId,
            fragmentManager = fragmentManager,
            isBlocksDownloaded = isBlocksDownloaded,
            onlyVideoBlocks = onlyVideoBlocks,
            removeDownloadModels = removeDownloadModels,
            saveDownloadModels = saveDownloadModels
        )
    }

    fun showRemoveDownloadModelPopup(
        downloadDialogItem: DownloadDialogItem,
        fragmentManager: FragmentManager,
        removeDownloadModels: () -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = listOf(downloadDialogItem),
                    isAllBlocksDownloaded = true,
                    isDownloadFailed = false,
                    sizeSum = downloadDialogItem.size,
                    fragmentManager = fragmentManager,
                    removeDownloadModels = removeDownloadModels,
                    saveDownloadModels = {}
                )
            )
        }
    }

    fun showDownloadFailedPopup(
        downloadModel: List<DownloadModel>,
        fragmentManager: FragmentManager,
    ) {
        createDownloadItems(
            downloadModel = downloadModel,
            fragmentManager = fragmentManager,
        )
    }

    private fun createDownloadItems(
        downloadModel: List<DownloadModel>,
        fragmentManager: FragmentManager,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val courseIds = downloadModel.map { it.courseId }.distinct()
            val blockIds = downloadModel.map { it.id }
            val notDownloadedSubSections = mutableListOf<Block>()
            val allDownloadDialogItems = mutableListOf<DownloadDialogItem>()
            courseIds.forEach { courseId ->
                val courseStructure = interactor.getCourseStructureFromCache(courseId)
                val allSubSectionBlocks = courseStructure.blockData.filter { it.type == BlockType.SEQUENTIAL }
                allSubSectionBlocks.forEach { subSectionsBlock ->
                    val verticalBlocks = courseStructure.blockData.filter { it.id in subSectionsBlock.descendants }
                    val blocks = courseStructure.blockData.filter {
                        it.id in verticalBlocks.flatMap { it.descendants } && it.id in blockIds
                    }
                    val size = blocks.sumOf { getFileSize(it) }
                    if (blocks.isNotEmpty()) notDownloadedSubSections.add(subSectionsBlock)
                    if (size > 0) {
                        val downloadDialogItem = DownloadDialogItem(
                            title = subSectionsBlock.displayName,
                            size = size
                        )
                        allDownloadDialogItems.add(downloadDialogItem)
                    }
                }
            }
            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = allDownloadDialogItems,
                    isAllBlocksDownloaded = false,
                    isDownloadFailed = true,
                    sizeSum = allDownloadDialogItems.sumOf { it.size },
                    fragmentManager = fragmentManager,
                    removeDownloadModels = {},
                    saveDownloadModels = {
                        CoroutineScope(Dispatchers.IO).launch {
                            workerController.saveModels(downloadModel)
                        }
                    }
                )
            )
        }
    }

    private fun createDownloadItems(
        subSectionsBlocks: List<Block>,
        courseId: String,
        fragmentManager: FragmentManager,
        isBlocksDownloaded: Boolean,
        onlyVideoBlocks: Boolean,
        removeDownloadModels: (blockId: String) -> Unit,
        saveDownloadModels: (blockId: String) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val courseStructure = interactor.getCourseStructure(courseId, false)
            val downloadModelIds = interactor.getAllDownloadModels().map { it.id }

            val downloadDialogItems = subSectionsBlocks.mapNotNull { subSectionsBlock ->
                val verticalBlocks = courseStructure.blockData.filter { it.id in subSectionsBlock.descendants }
                val blocks = verticalBlocks.flatMap { verticalBlock ->
                    courseStructure.blockData.filter {
                        it.id in verticalBlock.descendants &&
                                (isBlocksDownloaded == (it.id in downloadModelIds)) &&
                                (!onlyVideoBlocks || it.type == BlockType.VIDEO)
                    }
                }
                val size = blocks.sumOf { getFileSize(it) }
                if (size > 0) DownloadDialogItem(title = subSectionsBlock.displayName, size = size) else null
            }

            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = downloadDialogItems,
                    isAllBlocksDownloaded = isBlocksDownloaded,
                    isDownloadFailed = false,
                    sizeSum = downloadDialogItems.sumOf { it.size },
                    fragmentManager = fragmentManager,
                    removeDownloadModels = { subSectionsBlocks.forEach { removeDownloadModels(it.id) } },
                    saveDownloadModels = { subSectionsBlocks.forEach { saveDownloadModels(it.id) } }
                )
            )
        }
    }


    private fun getFileSize(block: Block): Long {
        return when {
            block.type == BlockType.VIDEO -> block.downloadModel?.size ?: 0
            block.isxBlock -> block.offlineDownload?.fileSize ?: 0
            else -> 0
        }
    }
}
