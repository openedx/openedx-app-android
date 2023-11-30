package org.openedx.course.presentation.videos

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor

class CourseVideoViewModel(
    val courseId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val notifier: CourseNotifier,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    private val _apiHostUrl = MutableLiveData<String>()
    val apiHostUrl: LiveData<String>
        get() = _apiHostUrl

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
        _apiHostUrl.value = config.getApiHostURL()
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