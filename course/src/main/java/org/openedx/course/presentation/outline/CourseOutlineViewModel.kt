package org.openedx.course.presentation.outline

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.R as courseR

class CourseOutlineViewModel(
    val courseId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val analytics: CourseAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    private val _apiHostUrl = MutableLiveData<String>()
    val apiHostUrl: LiveData<String>
        get() = _apiHostUrl

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

    var resumeSectionBlock: Block? = null
        private set
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
        _apiHostUrl.value = config.getApiHostURL()
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
            try {
                var courseStructure = interactor.getCourseStructureFromCache()
                val blocks = courseStructure.blockData

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
        resumeSectionBlock = blocks.find {
            it.descendants.contains(resumeVerticalBlock?.id) && it.type == BlockType.SEQUENTIAL
        }
        return resumeVerticalBlock
    }

    fun resumeCourseTappedEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseOutlineUIState.CourseData) {
            analytics.resumeCourseTappedEvent(courseId, currentState.courseStructure.name, blockId)
        }
    }

    fun sequentialClickedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseOutlineUIState.CourseData) {
            analytics.sequentialClickedEvent(
                courseId,
                currentState.courseStructure.name,
                blockId,
                blockName
            )
        }
    }

}