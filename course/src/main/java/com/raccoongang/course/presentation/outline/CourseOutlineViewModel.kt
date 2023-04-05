package com.raccoongang.course.presentation.outline

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
import com.raccoongang.core.domain.model.CourseComponentStatus
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.core.module.download.BaseDownloadViewModel
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseStructureUpdated
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.launch
import com.raccoongang.course.R as courseR

class CourseOutlineViewModel(
    val courseId: String,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: PreferencesManager,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    private val _uiState = MutableLiveData<CourseOutlineUIState>(CourseOutlineUIState.Loading)
    val uiState: LiveData<CourseOutlineUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    var courseTitle = ""

    var resumeVerticalBlock: Block? = null
        private set

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseStructureUpdated) {
                    if (event.courseId == courseId) {
                        updateCourseData(event.withSwipeRefresh)
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseOutlineUIState.CourseData) {
                    val state = _uiState.value as CourseOutlineUIState.CourseData
                    _uiState.value = CourseOutlineUIState.CourseData(
                        courseStructure = state.courseStructure,
                        downloadedState = it.toMap(),
                        resumeBlock = state.resumeBlock
                    )
                }
            }
        }
    }

    init {
        getCourseData()
    }

    fun setIsUpdating() {
        _isUpdating.value = true
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                _uiMessage.value =
                    UIMessage.ToastMessage(resourceManager.getString(courseR.string.course_can_download_only_with_wifi))
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    fun updateCourseData(withSwipeRefresh: Boolean) {
        _isUpdating.value = withSwipeRefresh
        getCourseDataInternal()
    }

    fun getCourseData() {
        _uiState.value = CourseOutlineUIState.Loading
        getCourseDataInternal()
    }

    private fun getCourseDataInternal() {
        viewModelScope.launch {
            var courseStructure = interactor.getCourseStructureFromCache()
            val blocks = courseStructure.blockData

            try {
                val courseStatus = if (networkConnection.isOnline()) {
                    interactor.getCourseStatus(courseId)
                } else {
                    CourseComponentStatus("")
                }
                setBlocks(blocks)
                courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                initDownloadModelsStatus()

                _uiState.value = CourseOutlineUIState.CourseData(
                    courseStructure = courseStructure,
                    downloadedState = getDownloadModelsStatus(),
                    resumeBlock = getResumeBlock(blocks, courseStatus.lastVisitedBlockId)
                )
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value = UIMessage.SnackBarMessage(
                        resourceManager.getString(R.string.core_error_no_connection)
                    )
                } else {
                    _uiMessage.value = UIMessage.SnackBarMessage(
                        resourceManager.getString(R.string.core_error_unknown_error)
                    )
                }
            }
            _isUpdating.value = false
        }
    }

    private fun sortBlocks(blocks: List<Block>): List<Block> {
        val resultBlocks = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        blocks.forEach { block ->
            if (block.type == BlockType.CHAPTER) {
                resultBlocks.add(block)
                block.descendants.forEach { descendant ->
                    blocks.find { it.id == descendant }?.let { sequentialBlock ->
                        resultBlocks.add(sequentialBlock)
                        addDownloadableChildrenForSequentialBlock(sequentialBlock)
                    }
                }
            }
        }
        return resultBlocks.toList()
    }

    private fun getResumeBlock(
        blocks: List<Block>,
        continueBlockId: String
    ): Block? {
        val resumeBlock = blocks.firstOrNull { it.id == continueBlockId }
        resumeVerticalBlock = blocks.find {
            it.descendants.contains(resumeBlock?.id) && it.type == BlockType.VERTICAL
        }
        return resumeBlock
    }

}