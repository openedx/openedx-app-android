package org.openedx.course.presentation.section

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSectionChanged
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import kotlinx.coroutines.launch
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.course.presentation.CourseAnalyticEvent
import org.openedx.course.presentation.CourseAnalyticKey
import org.openedx.course.presentation.CourseAnalyticValue

class CourseSectionViewModel(
    val courseId: String,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
    private val coreAnalytics: CoreAnalytics,
    workerController: DownloadWorkerController,
    downloadDao: DownloadDao,
) : BaseDownloadViewModel(courseId, downloadDao, preferencesManager, workerController, coreAnalytics) {

    private val _uiState = MutableLiveData<CourseSectionUIState>(CourseSectionUIState.Loading)
    val uiState: LiveData<CourseSectionUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    var mode = CourseViewMode.FULL

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            downloadModelsStatusFlow.collect { downloadModels ->
                when (val state = uiState.value) {
                    is CourseSectionUIState.Blocks -> {
                        val list = (uiState.value as CourseSectionUIState.Blocks).blocks
                        _uiState.value = CourseSectionUIState.Blocks(
                            sectionName = state.sectionName,
                            courseName = state.courseName,
                            blocks = ArrayList(list),
                            downloadedState = downloadModels.toMap())
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseSectionChanged) {
                    getBlocks(event.blockId, mode)
                }
            }
        }
    }

    fun getBlocks(blockId: String, mode: CourseViewMode) {
        _uiState.value = CourseSectionUIState.Loading
        viewModelScope.launch {
            try {
                val courseStructure = when (mode) {
                    CourseViewMode.FULL -> interactor.getCourseStructureFromCache()
                    CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos()
                }
                val blocks = courseStructure.blockData
                setBlocks(blocks)
                val newList = getDescendantBlocks(blocks, blockId)
                val sequentialBlock = getSequentialBlock(blocks, blockId)
                initDownloadModelsStatus()
                _uiState.value =
                    CourseSectionUIState.Blocks(
                        blocks = ArrayList(newList),
                        downloadedState = getDownloadModelsStatus(),
                        courseName = courseStructure.name,
                        sectionName = sequentialBlock.displayName
                    )
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                _uiMessage.value =
                    UIMessage.ToastMessage(resourceManager.getString(org.openedx.course.R.string.course_can_download_only_with_wifi))
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    private fun getDescendantBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = getSequentialBlock(blocks, id)
        for (descendant in selectedBlock.descendants) {
            val blockDescendant = blocks.find {
                it.id == descendant
            }
            if (blockDescendant != null) {
                if (blockDescendant.type == BlockType.VERTICAL) {
                    resultList.add(blockDescendant)
                    addDownloadableChildrenForVerticalBlock(blockDescendant)
                }
            } else continue
        }
        return resultList
    }

    private fun getSequentialBlock(blocks: List<Block>, id: String): Block {
        return blocks.first {
            it.id == id
        }
    }

    fun verticalClickedEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseSectionUIState.Blocks) {
            analytics.logEvent(
                CourseAnalyticEvent.UNIT_DETAIL.event,
                buildMap {
                    put(CourseAnalyticKey.NAME.key, CourseAnalyticValue.UNIT_DETAIL.biValue)
                    put(CourseAnalyticKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticKey.BLOCK_ID.key, blockId)
                    put(CourseAnalyticKey.CATEGORY.key, CourseAnalyticKey.NAVIGATION.key)
                })
        }
    }
}