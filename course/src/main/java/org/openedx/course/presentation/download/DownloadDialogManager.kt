package org.openedx.course.presentation.download

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.domain.interactor.CourseInteractor

class DownloadDialogManager(
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val interactor: CourseInteractor,
) {

    companion object {
        const val MAX_CELLURAL_SIZE = 100000000 // 100MB
    }

    private val uiState = MutableSharedFlow<DownloadDialogUIState>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            uiState.collect { uiState ->
                    when {
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

                        !corePreferences.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected() && uiState.sizeSum >= MAX_CELLURAL_SIZE -> {
                            val dialog = DownloadConfirmDialogFragment.newInstance(
                                dialogType = DownloadConfirmDialogType.DOWNLOAD_ON_CELLULAR,
                                uiState = uiState
                            )
                            dialog.show(
                                uiState.fragmentManager,
                                DownloadConfirmDialogFragment.DIALOG_TAG
                            )
                        }

                        else -> {
                            val dialog = DownloadConfirmDialogFragment.newInstance(
                                dialogType = DownloadConfirmDialogType.CONFIRM,
                                uiState = uiState
                            )
                            dialog.show(
                                uiState.fragmentManager,
                                DownloadConfirmDialogFragment.DIALOG_TAG
                            )
                        }
                }
            }
        }
    }

    fun showPopup(
        subSectionsBlocks: List<Block>,
        courseId: String,
        isAllBlocksDownloaded: Boolean,
        fragmentManager: FragmentManager,
        removeDownloadModels: (blockId: String) -> Unit,
        saveDownloadModels: (blockId: String) -> Unit,
    ) {
        getDownloadItems(
            subSectionsBlocks = subSectionsBlocks,
            courseId = courseId,
            fragmentManager = fragmentManager,
            isAllBlocksDownloaded = isAllBlocksDownloaded,
            removeDownloadModels = removeDownloadModels,
            saveDownloadModels = saveDownloadModels
        )
    }

    private fun getDownloadItems(
        subSectionsBlocks: List<Block>,
        courseId: String,
        fragmentManager: FragmentManager,
        isAllBlocksDownloaded: Boolean,
        removeDownloadModels: (blockId: String) -> Unit,
        saveDownloadModels: (blockId: String) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val courseStructure = interactor.getCourseStructure(courseId, false)
            val downloadDialogItems = subSectionsBlocks.mapNotNull { subSectionsBlock ->
                val verticalBlocks = courseStructure.blockData.filter { it.id in subSectionsBlock.descendants }
                val blocks = courseStructure.blockData.filter { it.id in verticalBlocks.flatMap { it.descendants } }
                val size = blocks.mapNotNull { it.downloadModel?.size }.sum().toLong()
                if (size > 0) {
                    DownloadDialogItem(title = subSectionsBlock.displayName, size = size)
                } else {
                    null
                }
            }
            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = downloadDialogItems,
                    isAllBlocksDownloaded = isAllBlocksDownloaded,
                    sizeSum = downloadDialogItems.sumOf { it.size },
                    fragmentManager = fragmentManager,
                    removeDownloadModels = {
                        subSectionsBlocks.forEach {
                            removeDownloadModels(it.id)
                        }
                    },
                    saveDownloadModels = {
                        subSectionsBlocks.forEach {
                            saveDownloadModels(it.id)
                        }
                    }
                )
            )
        }
    }
}
