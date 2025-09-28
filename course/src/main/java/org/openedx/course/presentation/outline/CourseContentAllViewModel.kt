package org.openedx.course.presentation.outline

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.extension.getChapterBlocks
import org.openedx.core.extension.getSequentialBlocks
import org.openedx.core.extension.getVerticalBlocks
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil
import org.openedx.course.R as courseR

class CourseContentAllViewModel(
    val courseId: String,
    private val courseTitle: String,
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

    private val _uiState =
        MutableStateFlow<CourseContentAllUIState>(CourseContentAllUIState.Loading)
    val uiState: StateFlow<CourseContentAllUIState>
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

    private var isOfflineBlocksUpToDate = false

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
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseContentAllUIState.CourseData) {
                    val state = _uiState.value as CourseContentAllUIState.CourseData
                    _uiState.value = CourseContentAllUIState.CourseData(
                        courseStructure = state.courseStructure,
                        downloadedState = it.toMap(),
                        resumeComponent = state.resumeComponent,
                        resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
                        courseSubSections = courseSubSections,
                        courseSectionsState = state.courseSectionsState,
                        subSectionsDownloadsCount = subSectionsDownloadsCount,
                        datesBannerInfo = state.datesBannerInfo,
                        useRelativeDates = preferencesManager.isRelativeDatesEnabled
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

    fun switchCourseSections(blockId: String): Boolean {
        return if (_uiState.value is CourseContentAllUIState.CourseData) {
            val state = _uiState.value as CourseContentAllUIState.CourseData
            val courseSectionsState = state.courseSectionsState.toMutableMap()
            courseSectionsState[blockId] = !(state.courseSectionsState[blockId] ?: false)

            _uiState.value = CourseContentAllUIState.CourseData(
                courseStructure = state.courseStructure,
                downloadedState = state.downloadedState,
                resumeComponent = state.resumeComponent,
                resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
                courseSubSections = courseSubSections,
                courseSectionsState = courseSectionsState,
                subSectionsDownloadsCount = subSectionsDownloadsCount,
                datesBannerInfo = state.datesBannerInfo,
                useRelativeDates = preferencesManager.isRelativeDatesEnabled
            )

            courseSectionsState[blockId] ?: false
        } else {
            false
        }
    }

    private fun getCourseDataInternal() {
        viewModelScope.launch {
            val courseStructureFlow = interactor.getCourseStructureFlow(courseId, false)
                .catch { emit(null) }
            val courseStatusFlow = interactor.getCourseStatusFlow(courseId)
            val courseDatesFlow = interactor.getCourseDatesFlow(courseId)
            combine(
                courseStructureFlow,
                courseStatusFlow,
                courseDatesFlow
            ) { courseStructure, courseStatus, courseDatesResult ->
                Triple(courseStructure, courseStatus, courseDatesResult)
            }.catch { e ->
                handleCourseDataError(e)
            }.collect { (courseStructure, courseStatus, courseDates) ->
                if (courseStructure == null) return@collect
                val blocks = courseStructure.blockData
                val datesBannerInfo = courseDates.courseBanner

                checkIfCalendarOutOfDate(courseDates.datesSection.values.flatten())
                updateOutdatedOfflineXBlocks(courseStructure)

                initializeCourseData(blocks, courseStructure, courseStatus, datesBannerInfo)
            }
        }
    }

    private suspend fun initializeCourseData(
        blocks: List<Block>,
        courseStructure: CourseStructure,
        courseStatus: CourseComponentStatus,
        datesBannerInfo: CourseDatesBannerInfo
    ) {
        setBlocks(blocks)
        courseSubSections.clear()
        courseSubSectionUnit.clear()
        val sortedStructure = courseStructure.copy(blockData = sortBlocks(blocks))
        initDownloadModelsStatus()

        val courseSectionsState =
            (_uiState.value as? CourseContentAllUIState.CourseData)?.courseSectionsState
                ?: blocks.getChapterBlocks().associate { it.id to !it.isCompleted() }

        _uiState.value = CourseContentAllUIState.CourseData(
            courseStructure = sortedStructure,
            downloadedState = getDownloadModelsStatus(),
            resumeComponent = getResumeBlock(blocks, courseStatus.lastVisitedBlockId),
            resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
            courseSubSections = courseSubSections,
            courseSectionsState = courseSectionsState,
            subSectionsDownloadsCount = subSectionsDownloadsCount,
            datesBannerInfo = datesBannerInfo,
            useRelativeDates = preferencesManager.isRelativeDatesEnabled
        )
    }

    private suspend fun handleCourseDataError(e: Throwable?) {
        _uiState.value = CourseContentAllUIState.Error
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
        }
    }

    private fun addSequentialBlockToSubSections(block: Block, sequentialBlock: Block) {
        courseSubSections.getOrPut(block.id) { mutableListOf() }.add(sequentialBlock)
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

    fun resetCourseDatesBanner() {
        viewModelScope.launch {
            try {
                interactor.resetCourseDates(courseId = courseId)
                getCourseData()
                courseNotifier.send(CourseDatesShifted)
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
        if (currentState is CourseContentAllUIState.CourseData) {
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

    fun sequentialClickedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseContentAllUIState.CourseData) {
            analytics.sequentialClickedEvent(
                courseId,
                currentState.courseStructure.name,
                blockId,
                blockName
            )
        }
    }

    fun logUnitDetailViewedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseContentAllUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.UNIT_DETAIL.eventName,
                buildMap {
                    put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.UNIT_DETAIL.biValue)
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                    put(CourseAnalyticsKey.BLOCK_NAME.key, blockName)
                }
            )
        }
    }

    private fun checkIfCalendarOutOfDate(courseDates: List<CourseDateBlock>) {
        viewModelScope.launch {
            courseNotifier.send(
                CreateCalendarSyncEvent(
                    courseDates = courseDates,
                    dialogType = CalendarSyncDialogType.NONE.name,
                    checkOutOfSync = true,
                )
            )
        }
    }

    fun downloadBlocks(blocksIds: List<String>, fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val courseData = _uiState.value as? CourseContentAllUIState.CourseData ?: return@launch

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

    private fun updateOutdatedOfflineXBlocks(courseStructure: CourseStructure) {
        viewModelScope.launch {
            if (!isOfflineBlocksUpToDate) {
                val xBlocks = courseStructure.blockData.filter { it.isxBlock }
                if (xBlocks.isNotEmpty()) {
                    val xBlockIds = xBlocks.map { it.id }.toSet()
                    val savedDownloadModelsMap = interactor.getAllDownloadModels()
                        .filter { it.id in xBlockIds }
                        .associateBy { it.id }

                    val outdatedBlockIds = xBlocks
                        .filter { block ->
                            val savedBlock = savedDownloadModelsMap[block.id]
                            savedBlock != null && block.offlineDownload?.lastModified != savedBlock.lastModified
                        }
                        .map { it.id }

                    outdatedBlockIds.forEach { blockId ->
                        interactor.removeDownloadModel(blockId)
                    }
                    saveDownloadModels(
                        fileUtil.getExternalAppDir().path,
                        courseId,
                        outdatedBlockIds
                    )
                }
                isOfflineBlocksUpToDate = true
            }
        }
    }
}
