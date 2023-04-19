package com.raccoongang.course.presentation.videos

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BlockType
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.core.module.download.BaseDownloadViewModel
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseStructureUpdated
import com.raccoongang.course.R
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.launch

class CourseVideoViewModel(
    val courseId: String,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: PreferencesManager,
    private val notifier: CourseNotifier,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    private val _uiState = MutableLiveData<CourseVideosUIState>()
    val uiState: LiveData<CourseVideosUIState>
        get() = _uiState

    var courseTitle = ""

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseStructureUpdated) {
                    if (event.courseId == courseId) {
                        updateVideos()
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseVideosUIState.CourseData) {
                    val state = _uiState.value as CourseVideosUIState.CourseData
                    _uiState.value = CourseVideosUIState.CourseData(
                        courseStructure = state.courseStructure,
                        downloadedState = it.toMap()
                    )
                }
            }
        }
    }

    init {
        getVideos()
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                _uiMessage.value =
                    UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi))
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    fun setIsUpdating() {
        _isUpdating.value = true
    }

    private fun updateVideos() {
        getVideos()
        _isUpdating.value = false
    }

    fun getVideos() {
        viewModelScope.launch {
            var courseStructure = interactor.getCourseStructureForVideos()
            val blocks = courseStructure.blockData
            if (blocks.isEmpty()) {
                _uiState.value = CourseVideosUIState.Empty(
                    message = resourceManager.getString(R.string.course_does_not_include_videos)
                )
            } else {
                setBlocks(courseStructure.blockData)
                courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                initDownloadModelsStatus()
                _uiState.value =
                    CourseVideosUIState.CourseData(courseStructure, getDownloadModelsStatus())
            }
        }
    }

    private fun sortBlocks(blocks: List<Block>): List<Block> {
        val resultBlocks = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        blocks.forEach { block ->
            if (block.type == BlockType.CHAPTER) {
                resultBlocks.add(block)
                block.descendants.forEach { descendant ->
                    blocks.find { it.id == descendant }?.let {
                        resultBlocks.add(it)
                        addDownloadableChildrenForSequentialBlock(it)
                    }
                }
            }
        }
        return resultBlocks.toList()
    }


}