package org.openedx.course.presentation.videos

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.VideoNotifier
import org.openedx.core.system.notifier.VideoQualityChanged
import org.openedx.core.utils.FileUtil
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseRouter

class CourseVideoViewModel(
    val courseId: String,
    val courseTitle: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val courseNotifier: CourseNotifier,
    private val videoNotifier: VideoNotifier,
    private val analytics: CourseAnalytics,
    val courseRouter: CourseRouter,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(
    courseId,
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics
) {

    val isCourseNestedListEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    private val _uiState = MutableStateFlow<CourseVideosUIState>(CourseVideosUIState.Loading)
    val uiState: StateFlow<CourseVideosUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _videoSettings = MutableStateFlow(VideoSettings.default)
    val videoSettings = _videoSettings.asStateFlow()

    private val courseSubSections = mutableMapOf<String, MutableList<Block>>()
    private val subSectionsDownloadsCount = mutableMapOf<String, Int>()
    val courseSubSectionUnit = mutableMapOf<String, Block?>()

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseStructureUpdated -> {
                        if (event.courseId == courseId) {
                            getVideos()
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseVideosUIState.CourseData) {
                    val state = _uiState.value as CourseVideosUIState.CourseData
                    _uiState.value = state.copy(
                        downloadedState = it.toMap(),
                        downloadModelsSize = getDownloadModelsSize()
                    )
                }
            }
        }

        viewModelScope.launch {
            videoNotifier.notifier.collect { event ->
                if (event is VideoQualityChanged) {
                    _videoSettings.value = preferencesManager.videoSettings

                    if (_uiState.value is CourseVideosUIState.CourseData) {
                        val state = _uiState.value as CourseVideosUIState.CourseData
                        _uiState.value = state.copy(
                            downloadModelsSize = getDownloadModelsSize()
                        )
                    }
                }
            }
        }

        _videoSettings.value = preferencesManager.videoSettings

        getVideos()
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                viewModelScope.launch {
                    _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi)))
                }
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    override fun saveAllDownloadModels(folder: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected()) {
            viewModelScope.launch {
                _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi)))
            }
            return
        }

        super.saveAllDownloadModels(folder)
    }

    fun getVideos() {
        viewModelScope.launch {
            var courseStructure = interactor.getCourseStructureForVideos(courseId)
            val blocks = courseStructure.blockData
            if (blocks.isEmpty()) {
                _uiState.value = CourseVideosUIState.Empty
            } else {
                setBlocks(courseStructure.blockData)
                courseSubSections.clear()
                courseSubSectionUnit.clear()
                courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                initDownloadModelsStatus()

                val courseSectionsState =
                    (_uiState.value as? CourseVideosUIState.CourseData)?.courseSectionsState.orEmpty()

                _uiState.value =
                    CourseVideosUIState.CourseData(
                        courseStructure, getDownloadModelsStatus(), courseSubSections,
                        courseSectionsState, subSectionsDownloadsCount, getDownloadModelsSize()
                    )
            }
            courseNotifier.send(CourseLoading(false))
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

    fun onChangingVideoQualityWhileDownloading() {
        viewModelScope.launch {
            _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.course_change_quality_when_downloading)))
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
                        courseSubSections.getOrPut(block.id) { mutableListOf() }
                            .add(it)
                        courseSubSectionUnit[it.id] = it.getFirstDescendantBlock(blocks)
                        subSectionsDownloadsCount[it.id] = it.getDownloadsCount(blocks)
                        addDownloadableChildrenForSequentialBlock(it)
                    }
                }
            }
        }
        return resultBlocks.toList()
    }

    fun downloadBlocks(
        blocksIds: List<String>,
        fragmentManager: FragmentManager,
        context: Context
    ) {
        if (blocksIds.find { isBlockDownloading(it) } != null) {
            courseRouter.navigateToDownloadQueue(fm = fragmentManager)
            return
        }
        blocksIds.forEach { blockId ->
            if (isBlockDownloaded(blockId)) {
                removeDownloadModels(blockId)
            } else {
                saveDownloadModels(
                    FileUtil(context).getExternalAppDir().path, blockId
                )
            }
        }
    }
}
