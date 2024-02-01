package org.openedx.course.presentation.videos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.DownloadProgressChanged
import org.openedx.core.system.notifier.ProfileNotifier
import org.openedx.core.system.notifier.VideoQualityChanged
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics

class CourseVideoViewModel(
    val courseId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val courseNotifier: CourseNotifier,
    private val profileNotifier: ProfileNotifier,
    private val downloadNotifier: DownloadNotifier,
    private val analytics: CourseAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    val apiHostUrl get() = config.getApiHostURL()

    val isCourseNestedListEnabled get() = config.isCourseNestedListEnabled()

    val isCourseBannerEnabled get() = config.isCourseBannerEnabled()

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

    private val _videoSettings = MutableStateFlow(VideoSettings.default)
    val videoSettings = _videoSettings.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val courseSubSections = mutableMapOf<String, MutableList<Block>>()
    private val subSectionsDownloadsCount = mutableMapOf<String, Int>()
    val courseSubSectionUnit = mutableMapOf<String, Block?>()

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
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
                    val isAllBlocksDownloadedOrDownloading = isAllBlocksDownloadedOrDownloading()
                    val remainingSize = if (isAllBlocksDownloadedOrDownloading) {
                        state.allDownloadModulesState.remainingDownloadModelsSize
                    } else {
                        getRemainingDownloadModelsSize()
                    }

                    _uiState.value = state.copy(
                        downloadedState = it.toMap(),
                        allDownloadModulesState = AllDownloadModulesState(
                            isAllBlocksDownloadedOrDownloading = isAllBlocksDownloadedOrDownloading,
                            remainingDownloadModelsCount = getRemainingDownloadModelsCount(),
                            remainingDownloadModelsSize = remainingSize,
                            allDownloadModelsCount = getAllDownloadModelsCount(),
                            allDownloadModelsSize = getAllDownloadModelsSize()
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            profileNotifier.notifier.collectLatest { event ->
                if (event is VideoQualityChanged) {
                    _videoSettings.value = preferencesManager.videoSettings
                }
            }
        }

        viewModelScope.launch {
            downloadNotifier.notifier.collect { event ->
                if (event is DownloadProgressChanged) {
                    if (_uiState.value is CourseVideosUIState.CourseData) {
                        if (isAllBlocksDownloadedOrDownloading() &&
                            getRemainingDownloadModelsCount() > 0
                        ) {
                            val remainingSize = getRemainingDownloadModelsSize() - event.value
                            val state = _uiState.value as CourseVideosUIState.CourseData
                            _uiState.value = state.copy(
                                allDownloadModulesState = state.allDownloadModulesState.copy(
                                    remainingDownloadModelsSize = remainingSize
                                )
                            )
                        }
                    }
                }
            }
        }

        getVideos()

        _videoSettings.value = preferencesManager.videoSettings
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

    override fun saveAllDownloadModels(folder: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected()) {
            _uiMessage.value =
                UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi))
            return
        }

        super.saveAllDownloadModels(folder)
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
                courseSubSections.clear()
                courseSubSectionUnit.clear()
                courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                initDownloadModelsStatus()

                val courseSectionsState =
                    (_uiState.value as? CourseVideosUIState.CourseData)?.courseSectionsState.orEmpty()

                val allDownloadModulesState = AllDownloadModulesState(
                    isAllBlocksDownloadedOrDownloading = isAllBlocksDownloadedOrDownloading(),
                    remainingDownloadModelsCount = getRemainingDownloadModelsCount(),
                    remainingDownloadModelsSize = getRemainingDownloadModelsSize(),
                    allDownloadModelsCount = getAllDownloadModelsCount(),
                    allDownloadModelsSize = getAllDownloadModelsSize()
                )

                _uiState.value =
                    CourseVideosUIState.CourseData(
                        courseStructure, getDownloadModelsStatus(), courseSubSections,
                        courseSectionsState, subSectionsDownloadsCount, allDownloadModulesState
                    )
            }
        }
    }

    fun switchCourseSections(blockId: String) {
        if (_uiState.value is CourseVideosUIState.CourseData) {
            val state = _uiState.value as CourseVideosUIState.CourseData
            val courseSectionsState = state.courseSectionsState.toMutableMap()
            courseSectionsState[blockId] = !(state.courseSectionsState[blockId] ?: false)

            _uiState.value = state.copy(courseSectionsState = courseSectionsState)
        }
    }

    fun sequentialClickedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseVideosUIState.CourseData) {
            analytics.sequentialClickedEvent(courseId, courseTitle, blockId, blockName)
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
                        if (isCourseNestedListEnabled) {
                            courseSubSections.getOrPut(block.id) { mutableListOf() }
                                .add(it)
                            courseSubSectionUnit[it.id] = it.getFirstDescendantBlock(blocks)
                            subSectionsDownloadsCount[it.id] = it.getDownloadsCount(blocks)

                        } else {
                            resultBlocks.add(it)
                        }
                        addDownloadableChildrenForSequentialBlock(it)
                    }
                }
            }
        }
        return resultBlocks.toList()
    }
}
