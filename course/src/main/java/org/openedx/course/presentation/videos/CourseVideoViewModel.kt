package org.openedx.course.presentation.videos

import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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
import org.openedx.core.extension.safeDivBy
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil

@SuppressLint("StaticFieldLeak")
class CourseVideoViewModel(
    val courseId: String,
    private val context: Context,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val courseNotifier: CourseNotifier,
    private val downloadDialogManager: DownloadDialogManager,
    private val fileUtil: FileUtil,
    val courseRouter: CourseRouter,
    private val analytics: CourseAnalytics,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
    downloadHelper: DownloadHelper,
) : BaseDownloadViewModel(
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics,
    downloadHelper,
) {
    private val _uiState = MutableStateFlow<CourseVideoUIState>(CourseVideoUIState.Loading)
    val uiState: StateFlow<CourseVideoUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val courseVideos = mutableMapOf<String, MutableList<Block>>()
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
                if (_uiState.value is CourseVideoUIState.CourseData) {
                    val state = _uiState.value as CourseVideoUIState.CourseData
                    _uiState.value = state.copy(
                        downloadedState = it.toMap(),
                        downloadModelsSize = getDownloadModelsSize()
                    )
                }
            }
        }

        getVideos()
    }

    override fun saveDownloadModels(folder: String, courseId: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, courseId, id)
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
            super.saveDownloadModels(folder, courseId, id)
        }
    }

    override fun saveAllDownloadModels(folder: String, courseId: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected()) {
            viewModelScope.launch {
                _uiMessage.emit(
                    UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi))
                )
            }
            return
        }

        super.saveAllDownloadModels(folder, courseId)
    }

    fun getVideos() {
        viewModelScope.launch {
            try {
                var courseStructure = interactor.getCourseStructureForVideos(courseId)
                val blocks = courseStructure.blockData
                if (blocks.isEmpty()) {
                    _uiState.value = CourseVideoUIState.Empty
                } else {
                    setBlocks(courseStructure.blockData)
                    courseVideos.clear()
                    courseSubSectionUnit.clear()
                    courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                    initDownloadModelsStatus()
                    val videoProgress = courseVideos.values.flatten().associate { block ->
                        val videoProgressEntity = interactor.getVideoProgress(block.id)
                        val videoTime = videoProgressEntity.videoTime?.toFloat()
                        val videoDuration = videoProgressEntity.duration?.toFloat()
                        val progress = if (videoTime != null && videoDuration != null) {
                            videoTime.safeDivBy(videoDuration)
                        } else {
                            null
                        }
                        block.id to progress
                    }
                    val isCompletedSectionsShown =
                        (_uiState.value as? CourseVideoUIState.CourseData)?.isCompletedSectionsShown
                            ?: false

                    _uiState.value =
                        CourseVideoUIState.CourseData(
                            courseStructure = courseStructure,
                            downloadedState = getDownloadModelsStatus(),
                            courseVideos = courseVideos,
                            subSectionsDownloadsCount = subSectionsDownloadsCount,
                            downloadModelsSize = getDownloadModelsSize(),
                            isCompletedSectionsShown = isCompletedSectionsShown,
                            videoPreview = (_uiState.value as? CourseVideoUIState.CourseData)?.videoPreview
                                ?: emptyMap(),
                            videoProgress = videoProgress,
                        )
                }
                courseNotifier.send(CourseLoading(false))
                getVideoPreviews()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = CourseVideoUIState.Empty
            }
        }
    }

    private fun getVideoPreviews() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadingModels = getDownloadModelList()
            courseVideos.values.flatten().forEach { block ->
                val previewMap = block.id to block.getVideoPreview(
                    context,
                    networkConnection.isOnline(),
                    downloadingModels.find { block.id == it.id }?.path
                )
                val currentUiState =
                    (_uiState.value as? CourseVideoUIState.CourseData) ?: return@forEach
                _uiState.value = currentUiState.copy(
                    videoPreview = currentUiState.videoPreview + previewMap
                )
            }
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
            val verticalBlocks = blocks.filter { block ->
                block.id in sequentialBlock.descendants
            }
            val videoBlocks = blocks.filter { block ->
                verticalBlocks.any { vertical -> block.id in vertical.descendants } && block.type == BlockType.VIDEO
            }
            addToSubSections(chapterBlock, sequentialBlock)
            addToVideo(chapterBlock, videoBlocks)
            updateSubSectionUnit(sequentialBlock, blocks)
            updateDownloadsCount(sequentialBlock, blocks)
            addDownloadableChildrenForSequentialBlock(sequentialBlock)
        }
    }

    private fun addToSubSections(chapterBlock: Block, sequentialBlock: Block) {
        courseSubSections.getOrPut(chapterBlock.id) { mutableListOf() }.add(sequentialBlock)
    }

    private fun addToVideo(chapterBlock: Block, videoBlocks: List<Block>) {
        courseVideos.getOrPut(chapterBlock.id) { mutableListOf() }.addAll(videoBlocks)
    }

    private fun updateSubSectionUnit(sequentialBlock: Block, blocks: List<Block>) {
        courseSubSectionUnit[sequentialBlock.id] = sequentialBlock.getFirstDescendantBlock(blocks)
    }

    private fun updateDownloadsCount(sequentialBlock: Block, blocks: List<Block>) {
        subSectionsDownloadsCount[sequentialBlock.id] = sequentialBlock.getDownloadsCount(blocks)
    }

    fun downloadBlocks(blocksIds: List<String>, fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val subSectionsBlocks =
                courseSubSections.values.flatten().filter { it.id in blocksIds }

            val blocks = subSectionsBlocks.flatMap { subSectionsBlock ->
                val verticalBlocks =
                    allBlocks.values.filter { it.id in subSectionsBlock.descendants }
                allBlocks.values.filter { it.id in verticalBlocks.flatMap { it.descendants } }
            }

            val downloadableBlocks = blocks.filter { it.isDownloadable }
            val downloadingBlocks = blocksIds.filter { isBlockDownloading(it) }
            val isAllBlocksDownloaded = downloadableBlocks.all { isBlockDownloaded(it.id) }

            val notDownloadedSubSectionBlocks = subSectionsBlocks.mapNotNull { subSectionsBlock ->
                val verticalBlocks =
                    allBlocks.values.filter { it.id in subSectionsBlock.descendants }
                val notDownloadedBlocks = allBlocks.values.filter {
                    it.id in verticalBlocks.flatMap { it.descendants } && it.isDownloadable && !isBlockDownloaded(
                        it.id
                    )
                }
                if (notDownloadedBlocks.isNotEmpty()) subSectionsBlock else null
            }

            val requiredSubSections = notDownloadedSubSectionBlocks.ifEmpty {
                subSectionsBlocks
            }

            if (downloadingBlocks.isNotEmpty()) {
                val downloadableChildren =
                    downloadingBlocks.flatMap { getDownloadableChildren(it).orEmpty() }
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
                        saveDownloadModels(fileUtil.getExternalAppDir().path, courseId, blockId)
                    }
                )
            }
        }
    }

    fun onCompletedSectionVisibilityChange() {
        if (_uiState.value is CourseVideoUIState.CourseData) {
            val state = _uiState.value as CourseVideoUIState.CourseData
            _uiState.value = state.copy(isCompletedSectionsShown = !state.isCompletedSectionsShown)

            analytics.logEvent(
                CourseAnalyticsEvent.VIDEO_SHOW_COMPLETED.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.VIDEO_SHOW_COMPLETED.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                }
            )
        }
    }

    fun logVideoClick(blockId: String) {
        if (_uiState.value is CourseVideoUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_CONTENT_VIDEO_CLICK.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_CONTENT_VIDEO_CLICK.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                }
            )
        }
    }

    fun getBlockParent(blockId: String): Block? {
        return allBlocks.values.find { blockId in it.descendants }
    }
}
