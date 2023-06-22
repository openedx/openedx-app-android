package com.raccoongang.course.presentation.section

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BlockType
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.core.module.download.BaseDownloadViewModel
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseSectionChanged
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.launch

class CourseSectionViewModel(
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: PreferencesManager,
    private val notifier: CourseNotifier,
    workerController: DownloadWorkerController,
    downloadDao: DownloadDao,
    val courseId: String
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

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
                if (uiState.value is CourseSectionUIState.Blocks) {
                    val list = (uiState.value as CourseSectionUIState.Blocks).blocks
                    _uiState.value =
                        CourseSectionUIState.Blocks(ArrayList(list), downloadModels.toMap())
                }
            }
        }

        viewModelScope.launch {
            notifier.notifier.collect{event ->
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
                initDownloadModelsStatus()

                _uiState.value = CourseSectionUIState.Blocks(ArrayList(newList), getDownloadModelsStatus())
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
                    UIMessage.ToastMessage(resourceManager.getString(com.raccoongang.course.R.string.course_can_download_only_with_wifi))
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    private fun getDescendantBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = blocks.first {
            it.id == id
        }
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
}