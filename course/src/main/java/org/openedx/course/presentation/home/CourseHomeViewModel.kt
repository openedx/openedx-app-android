package org.openedx.course.presentation.home

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.extension.getChapterBlocks
import org.openedx.core.extension.getSequentialBlocks
import org.openedx.core.extension.getVerticalBlocks
import org.openedx.core.extension.safeDivBy
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseProgressLoaded
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import org.openedx.course.R as courseR

class CourseHomeViewModel(
    val courseId: String,
    private val courseTitle: String,
    private val context: Context,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val analytics: CourseAnalytics,
    private val downloadDialogManager: DownloadDialogManager,
    private val fileUtil: FileUtil,
    val courseRouter: CourseRouter,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
    downloadHelper: DownloadHelper,
) : BaseDownloadViewModel(
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics,
    downloadHelper
) {
    val isCourseDropdownNavigationEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    private val _uiState = MutableStateFlow<CourseHomeUIState>(CourseHomeUIState.Waiting)
    val uiState: StateFlow<CourseHomeUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _resumeBlockId = MutableSharedFlow<String>()
    val resumeBlockId: SharedFlow<String>
        get() = _resumeBlockId.asSharedFlow()

    private var resumeSectionBlock: Block? = null
    private var resumeVerticalBlock: Block? = null

    private val isCourseExpandableSectionsEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    private val courseSubSections = mutableMapOf<String, MutableList<Block>>()
    private val subSectionsDownloadsCount = mutableMapOf<String, Int>()
    val courseSubSectionUnit = mutableMapOf<String, Block?>()
    private val courseVideos = mutableMapOf<String, MutableList<Block>>()
    private val courseAssignments = mutableListOf<Block>()

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseStructureUpdated -> {
                        if (event.courseId == courseId) {
                            getCourseData()
                        }
                    }

                    is CourseOpenBlock -> {
                        _resumeBlockId.emit(event.blockId)
                    }

                    is CourseProgressLoaded -> {
                        getCourseProgress()
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseHomeUIState.CourseData) {
                    val state = _uiState.value as CourseHomeUIState.CourseData
                    _uiState.value = CourseHomeUIState.CourseData(
                        courseStructure = state.courseStructure,
                        downloadedState = it.toMap(),
                        resumeComponent = state.resumeComponent,
                        resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
                        courseSubSections = courseSubSections,
                        subSectionsDownloadsCount = subSectionsDownloadsCount,
                        datesBannerInfo = state.datesBannerInfo,
                        useRelativeDates = preferencesManager.isRelativeDatesEnabled,
                        next = state.next,
                        courseProgress = state.courseProgress,
                        courseVideos = courseVideos,
                        courseAssignments = courseAssignments,
                        videoPreview = state.videoPreview,
                        videoProgress = state.videoProgress
                    )
                }
            }
        }

        getCourseData()
    }

    override fun saveDownloadModels(folder: String, courseId: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, courseId, id)
            } else {
                viewModelScope.launch {
                    _uiMessage.emit(
                        UIMessage.ToastMessage(
                            resourceManager.getString(courseR.string.course_can_download_only_with_wifi)
                        )
                    )
                }
            }
        } else {
            super.saveDownloadModels(folder, courseId, id)
        }
    }

    fun getCourseData() {
        getCourseDataInternal()
    }

    private fun getCourseDataInternal() {
        viewModelScope.launch {
            val courseStructureFlow = interactor.getCourseStructureFlow(courseId, false)
                .catch { emit(null) }
            val courseStatusFlow = interactor.getCourseStatusFlow(courseId)
            val courseDatesFlow = interactor.getCourseDatesFlow(courseId)
            val courseProgressFlow = interactor.getCourseProgress(courseId, false, true)
            combine(
                courseStructureFlow,
                courseStatusFlow,
                courseDatesFlow,
                courseProgressFlow
            ) { courseStructure, courseStatus, courseDatesResult, courseProgress ->
                if (courseStructure == null) return@combine
                val blocks = courseStructure.blockData
                val datesBannerInfo = courseDatesResult.courseBanner

                initializeCourseData(
                    blocks,
                    courseStructure,
                    courseStatus,
                    datesBannerInfo,
                    courseProgress
                )
            }.catch { e ->
                handleCourseDataError(e)
            }.collect { }
        }
    }

    private suspend fun initializeCourseData(
        blocks: List<Block>,
        courseStructure: CourseStructure,
        courseStatus: CourseComponentStatus,
        datesBannerInfo: CourseDatesBannerInfo,
        courseProgress: CourseProgress
    ) {
        setBlocks(blocks)
        courseSubSections.clear()
        courseSubSectionUnit.clear()
        courseVideos.clear()
        courseAssignments.clear()

        // Collect all assignments from the original blocks
        val allAssignments = blocks
            .filter { !it.assignmentProgress?.assignmentType.isNullOrEmpty() }
            .filter { it.graded }
            .sortedWith(
                compareBy<Block> { it.due == null }
                    .thenBy { it.due }
            )
        courseAssignments.addAll(allAssignments)

        sortBlocks(blocks)
        initDownloadModelsStatus()
        val nextSection = findFirstChapterWithIncompleteDescendants(blocks)

        // Get video data
        val allVideos = courseVideos.values.flatten()
        val firstIncompleteVideo = allVideos.find { !it.isCompleted() }
        val videoProgress = if (firstIncompleteVideo != null) {
            try {
                val videoProgressEntity = interactor.getVideoProgress(firstIncompleteVideo.id)
                val videoTime = videoProgressEntity.videoTime?.toFloat()
                val videoDuration = videoProgressEntity.duration?.toFloat()
                val progress = if (videoTime != null && videoDuration != null) {
                    videoTime.safeDivBy(videoDuration)
                } else {
                    null
                }
                progress?.coerceIn(0f, 1f)
            } catch (_: Exception) {
                0f
            }
        } else {
            0f
        }

        _uiState.value = CourseHomeUIState.CourseData(
            courseStructure = courseStructure,
            next = nextSection,
            downloadedState = getDownloadModelsStatus(),
            resumeComponent = getResumeBlock(blocks, courseStatus.lastVisitedBlockId),
            resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
            courseSubSections = courseSubSections,
            subSectionsDownloadsCount = subSectionsDownloadsCount,
            datesBannerInfo = datesBannerInfo,
            useRelativeDates = preferencesManager.isRelativeDatesEnabled,
            courseProgress = courseProgress,
            courseVideos = courseVideos,
            courseAssignments = courseAssignments,
            videoPreview = (_uiState.value as? CourseHomeUIState.CourseData)?.videoPreview,
            videoProgress = videoProgress
        )
        getVideoPreview(firstIncompleteVideo)
    }

    private fun getVideoPreview(videoBlock: Block?) {
        viewModelScope.launch(Dispatchers.IO) {
            val videoPreview = videoBlock?.getVideoPreview(
                context,
                networkConnection.isOnline(),
                null
            )
            _uiState.value = (_uiState.value as? CourseHomeUIState.CourseData)
                ?.copy(
                    videoPreview = videoPreview
                ) ?: return@launch
        }
    }

    private suspend fun handleCourseDataError(e: Throwable?) {
        _uiState.value = CourseHomeUIState.Error
        val errorMessage = when {
            e?.isInternetError() == true -> R.string.core_error_no_connection
            else -> R.string.core_error_unknown_error
        }
        _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(errorMessage)))
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

    private fun processDescendants(block: Block, blocks: List<Block>) {
        block.descendants.forEach { descendantId ->
            val sequentialBlock = blocks.find { it.id == descendantId } ?: return@forEach
            addSequentialBlockToSubSections(block, sequentialBlock)
            courseSubSectionUnit[sequentialBlock.id] =
                sequentialBlock.getFirstDescendantBlock(blocks)
            subSectionsDownloadsCount[sequentialBlock.id] =
                sequentialBlock.getDownloadsCount(blocks)
            addDownloadableChildrenForSequentialBlock(sequentialBlock)

            // Add video processing logic
            val verticalBlocks = blocks.filter { block ->
                block.id in sequentialBlock.descendants
            }
            val videoBlocks = blocks.filter { block ->
                verticalBlocks.any { vertical -> block.id in vertical.descendants } && block.type == BlockType.VIDEO
            }
            addToVideos(block, videoBlocks)
        }
    }

    private fun addSequentialBlockToSubSections(block: Block, sequentialBlock: Block) {
        courseSubSections.getOrPut(block.id) { mutableListOf() }.add(sequentialBlock)
    }

    private fun addToVideos(chapterBlock: Block, videoBlocks: List<Block>) {
        courseVideos.getOrPut(chapterBlock.id) { mutableListOf() }.addAll(videoBlocks)
    }

    fun getBlockParent(blockId: String): Block? {
        return allBlocks.values.find { blockId in it.descendants }
    }

    private fun getResumeBlock(
        blocks: List<Block>,
        continueBlockId: String,
    ): Block? {
        val resumeBlock = blocks.firstOrNull { it.id == continueBlockId }
        resumeVerticalBlock =
            blocks.getVerticalBlocks().find { it.descendants.contains(resumeBlock?.id) }
        resumeSectionBlock =
            blocks.getSequentialBlocks().find { it.descendants.contains(resumeVerticalBlock?.id) }
        return resumeBlock
    }

    /**
     * Finds the first chapter which has incomplete descendants and returns it as a Pair<Block, Block>
     * where the first Block is the chapter and the second Block is the first incomplete subsection
     */
    private fun findFirstChapterWithIncompleteDescendants(blocks: List<Block>): Pair<Block, Block>? {
        val incompleteChapterBlock = blocks.getChapterBlocks().find { !it.isCompleted() }
        val incompleteSubsection = incompleteChapterBlock?.let {
            findFirstIncompleteSubsection(it, blocks)
        }
        return if (incompleteChapterBlock != null && incompleteSubsection != null) {
            Pair(incompleteChapterBlock, incompleteSubsection)
        } else {
            null
        }
    }

    private fun findFirstIncompleteSubsection(chapter: Block, blocks: List<Block>): Block? {
        // Get all sequential blocks (subsections) in this chapter
        val sequentialBlocks = chapter.descendants.mapNotNull { descendantId ->
            blocks.find { it.id == descendantId && it.type == BlockType.SEQUENTIAL }
        }
        return sequentialBlocks.find { !it.isCompleted() }
    }

    fun resetCourseDatesBanner(onResetDates: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                interactor.resetCourseDates(courseId = courseId)
                getCourseData()
                courseNotifier.send(CourseDatesShifted)
                onResetDates(true)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_no_connection)
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_dates_shift_dates_unsuccessful_msg)
                        )
                    )
                }
                onResetDates(false)
            }
        }
    }

    fun openBlock(fragmentManager: FragmentManager, blockId: String) {
        viewModelScope.launch {
            val courseStructure = interactor.getCourseStructure(courseId, false)
            val blocks = courseStructure.blockData
            getResumeBlock(blocks, blockId)
            resumeBlock(fragmentManager, blockId)
        }
    }

    private fun resumeBlock(fragmentManager: FragmentManager, blockId: String) {
        resumeSectionBlock?.let { subSection ->
            resumeCourseTappedEvent(subSection.id)
            resumeVerticalBlock?.let { unit ->
                if (isCourseExpandableSectionsEnabled) {
                    courseRouter.navigateToCourseContainer(
                        fm = fragmentManager,
                        courseId = courseId,
                        unitId = unit.id,
                        componentId = blockId,
                        mode = CourseViewMode.FULL
                    )
                } else {
                    courseRouter.navigateToCourseSubsections(
                        fragmentManager,
                        courseId = courseId,
                        subSectionId = subSection.id,
                        mode = CourseViewMode.FULL,
                        unitId = unit.id,
                        componentId = blockId
                    )
                }
            }
        }
    }

    fun downloadBlocks(blocksIds: List<String>, fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val courseData = _uiState.value as? CourseHomeUIState.CourseData ?: return@launch

            val subSectionsBlocks =
                courseData.courseSubSections.values.flatten().filter { it.id in blocksIds }

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
                if (notDownloadedBlocks.isNotEmpty()) {
                    subSectionsBlock
                } else {
                    null
                }
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
                    fragmentManager = fragmentManager,
                    removeDownloadModels = ::removeDownloadModels,
                    saveDownloadModels = { blockId ->
                        saveDownloadModels(fileUtil.getExternalAppDir().path, courseId, blockId)
                    }
                )
            }
        }
    }

    fun getCourseProgress() {
        viewModelScope.launch {
            if (_uiState.value !is CourseHomeUIState.CourseData) {
                _uiState.value = CourseHomeUIState.Loading
            }
            interactor.getCourseProgress(courseId, false, true)
                .catch { e ->
                    if (_uiState.value !is CourseHomeUIState.CourseData) {
                        _uiState.value = CourseHomeUIState.Error
                    }
                }
                .collectLatest { progress ->
                    val currentState = _uiState.value
                    if (currentState is CourseHomeUIState.CourseData) {
                        _uiState.value = currentState.copy(courseProgress = progress)
                    }
                }
        }
    }

    fun logVideoClick(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_VIDEO_CLICK.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_VIDEO_CLICK.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                }
            )
        }
    }

    fun logAssignmentClick(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_ASSIGNMENT_CLICK.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_ASSIGNMENT_CLICK.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                }
            )
        }
    }

    fun viewCertificateTappedEvent() {
        analytics.logEvent(
            CourseAnalyticsEvent.VIEW_CERTIFICATE.eventName,
            buildMap {
                put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.VIEW_CERTIFICATE.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
            }
        )
    }

    private fun resumeCourseTappedEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.RESUME_COURSE_CLICKED.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.RESUME_COURSE_CLICKED.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                }
            )
        }
    }

    fun logSectionSubsectionClick(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_SECTION_SUBSECTION_CLICK.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_SECTION_SUBSECTION_CLICK.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                    put(CourseAnalyticsKey.BLOCK_NAME.key, blockName)
                }
            )
        }
    }

    fun logViewAllContentClick() {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_VIEW_ALL_CONTENT.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_VIEW_ALL_CONTENT.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                }
            )
        }
    }

    fun logViewAllVideosClick() {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_VIEW_ALL_VIDEOS.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_VIEW_ALL_VIDEOS.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                }
            )
        }
    }

    fun logViewAllAssignmentsClick() {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_VIEW_ALL_ASSIGNMENTS.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_VIEW_ALL_ASSIGNMENTS.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                }
            )
        }
    }

    fun logViewProgressClick() {
        val currentState = uiState.value
        if (currentState is CourseHomeUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.COURSE_HOME_GRADES_VIEW_PROGRESS.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.COURSE_HOME_GRADES_VIEW_PROGRESS.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, currentState.courseStructure.name)
                }
            )
        }
    }
}
