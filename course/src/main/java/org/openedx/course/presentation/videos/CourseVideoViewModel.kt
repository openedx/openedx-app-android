package org.openedx.course.presentation.videos

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
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.VideoNotifier
import org.openedx.core.system.notifier.VideoQualityChanged
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.download.DownloadDialogManager
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil

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
    private val downloadDialogManager: DownloadDialogManager,
    private val fileUtil: FileUtil,
    val courseRouter: CourseRouter,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
    downloadHelper: DownloadHelper,
) : BaseDownloadViewModel(
    courseId,
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics,
    downloadHelper,
) {

    val isCourseDropdownNavigationEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

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
                    _uiMessage.emit(
                        UIMessage.ToastMessage(
                            resourceManager.getString(R.string.course_can_download_only_with_wifi)
                        )
                    )
                }
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    override fun saveAllDownloadModels(folder: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected()) {
            viewModelScope.launch {
                _uiMessage.emit(
                    UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi))
                )
            }
            return
        }

        super.saveAllDownloadModels(folder)
    }

    fun getVideos() {
        viewModelScope.launch {
            try {
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
                            courseStructure = courseStructure,
                            downloadedState = getDownloadModelsStatus(),
                            courseSubSections = courseSubSections,
                            courseSectionsState = courseSectionsState,
                            subSectionsDownloadsCount = subSectionsDownloadsCount,
                            downloadModelsSize = getDownloadModelsSize(),
                            useRelativeDates = preferencesManager.isRelativeDatesEnabled
                        )
                }
                courseNotifier.send(CourseLoading(false))
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = CourseVideosUIState.Empty
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
            analytics.sequentialClickedEvent(
                courseId,
                courseTitle,
                blockId,
                blockName
            )
        }
    }

    fun onChangingVideoQualityWhileDownloading() {
        viewModelScope.launch {
            _uiMessage.emit(
                UIMessage.SnackBarMessage(
                    resourceManager.getString(R.string.course_change_quality_when_downloading)
                )
            )
        }
    }

    private fun sortBlocks(blocks: List<Block>): List<Block> {
        if (blocks.isEmpty()) return emptyList()

        val resultBlocks = mutableListOf<Block>()
        blocks.forEach { block ->
            if (block.type == BlockType.CHAPTER) {
                resultBlocks.add(block)
                processDescendants(block, blocks)
            }
        }
        return resultBlocks
    }

    private fun processDescendants(chapterBlock: Block, blocks: List<Block>) {
        chapterBlock.descendants.forEach { descendantId ->
            val sequentialBlock = blocks.find { it.id == descendantId } ?: return@forEach
            addToSubSections(chapterBlock, sequentialBlock)
            updateSubSectionUnit(sequentialBlock, blocks)
            updateDownloadsCount(sequentialBlock, blocks)
            addDownloadableChildrenForSequentialBlock(sequentialBlock)
        }
    }

    private fun addToSubSections(chapterBlock: Block, sequentialBlock: Block) {
        courseSubSections.getOrPut(chapterBlock.id) { mutableListOf() }.add(sequentialBlock)
    }

    private fun updateSubSectionUnit(sequentialBlock: Block, blocks: List<Block>) {
        courseSubSectionUnit[sequentialBlock.id] = sequentialBlock.getFirstDescendantBlock(blocks)
    }

    private fun updateDownloadsCount(sequentialBlock: Block, blocks: List<Block>) {
        subSectionsDownloadsCount[sequentialBlock.id] = sequentialBlock.getDownloadsCount(blocks)
    }

    fun downloadBlocks(blocksIds: List<String>, fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val courseData = _uiState.value as? CourseVideosUIState.CourseData ?: return@launch

            val subSectionsBlocks = courseData.courseSubSections.values.flatten().filter { it.id in blocksIds }

            val blocks = subSectionsBlocks.flatMap { subSectionsBlock ->
                val verticalBlocks = allBlocks.values.filter { it.id in subSectionsBlock.descendants }
                allBlocks.values.filter { it.id in verticalBlocks.flatMap { it.descendants } }
            }

            val downloadableBlocks = blocks.filter { it.isDownloadable }
            val downloadingBlocks = blocksIds.filter { isBlockDownloading(it) }
            val isAllBlocksDownloaded = downloadableBlocks.all { isBlockDownloaded(it.id) }

            val notDownloadedSubSectionBlocks = subSectionsBlocks.mapNotNull { subSectionsBlock ->
                val verticalBlocks = allBlocks.values.filter { it.id in subSectionsBlock.descendants }
                val notDownloadedBlocks = allBlocks.values.filter {
                    it.id in verticalBlocks.flatMap { it.descendants } && it.isDownloadable && !isBlockDownloaded(it.id)
                }
                if (notDownloadedBlocks.isNotEmpty()) subSectionsBlock else null
            }

            val requiredSubSections = notDownloadedSubSectionBlocks.ifEmpty {
                subSectionsBlocks
            }

            if (downloadingBlocks.isNotEmpty()) {
                val downloadableChildren = downloadingBlocks.flatMap { getDownloadableChildren(it).orEmpty() }
                if (config.getCourseUIConfig().isCourseDownloadQueueEnabled) {
                    courseRouter.navigateToDownloadQueue(fragmentManager, downloadableChildren)
                } else {
                    downloadableChildren.forEach {
                        if (!isBlockDownloaded(it)) {
                            removeBlockDownloadModel(it)
                        }
                    }
                }
            } else {
                downloadDialogManager.showPopup(
                    subSectionsBlocks = requiredSubSections,
                    courseId = courseId,
                    isBlocksDownloaded = isAllBlocksDownloaded,
                    onlyVideoBlocks = true,
                    fragmentManager = fragmentManager,
                    removeDownloadModels = ::removeDownloadModels,
                    saveDownloadModels = { blockId ->
                        saveDownloadModels(fileUtil.getExternalAppDir().path, blockId)
                    }
                )
            }
        }
    }
}
