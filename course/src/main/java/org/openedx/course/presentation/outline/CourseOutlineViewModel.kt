package org.openedx.course.presentation.outline

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.extension.getSequentialBlocks
import org.openedx.core.extension.getVerticalBlocks
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.R as courseR

class CourseOutlineViewModel(
    val courseId: String,
    private val courseTitle: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val analytics: CourseAnalytics,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
) : BaseDownloadViewModel(
    courseId,
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics
) {
    val isCourseNestedListEnabled get() = config.isCourseNestedListEnabled()

    private val _uiState = MutableStateFlow<CourseOutlineUIState>(CourseOutlineUIState.Loading)
    val uiState: StateFlow<CourseOutlineUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    var resumeSectionBlock: Block? = null
        private set
    var resumeVerticalBlock: Block? = null
        private set

    val isCourseExpandableSectionsEnabled get() = config.isCourseNestedListEnabled()

    private val courseSubSections = mutableMapOf<String, MutableList<Block>>()
    private val subSectionsDownloadsCount = mutableMapOf<String, Int>()
    val courseSubSectionUnit = mutableMapOf<String, Block?>()

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseStructureUpdated -> {
                        if (event.courseId == courseId) {
                            updateCourseData()
                        }
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
                        resumeComponent = state.resumeComponent,
                        courseSubSections = courseSubSections,
                        courseSectionsState = state.courseSectionsState,
                        subSectionsDownloadsCount = subSectionsDownloadsCount,
                        datesBannerInfo = state.datesBannerInfo,
                    )
                }
            }
        }

        getCourseData()
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                viewModelScope.launch {
                    _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(courseR.string.course_can_download_only_with_wifi)))
                }
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    fun updateCourseData() {
        getCourseDataInternal()
    }

    fun getCourseData() {
        viewModelScope.launch {
            courseNotifier.send(CourseLoading(true))
        }
        getCourseDataInternal()
    }

    fun switchCourseSections(blockId: String): Boolean {
        return if (_uiState.value is CourseOutlineUIState.CourseData) {
            val state = _uiState.value as CourseOutlineUIState.CourseData
            val courseSectionsState = state.courseSectionsState.toMutableMap()
            courseSectionsState[blockId] = !(state.courseSectionsState[blockId] ?: false)

            _uiState.value = CourseOutlineUIState.CourseData(
                courseStructure = state.courseStructure,
                downloadedState = state.downloadedState,
                resumeComponent = state.resumeComponent,
                courseSubSections = courseSubSections,
                courseSectionsState = courseSectionsState,
                subSectionsDownloadsCount = subSectionsDownloadsCount,
                datesBannerInfo = state.datesBannerInfo,
            )

            courseSectionsState[blockId] ?: false

        } else {
            false
        }
    }

    private fun getCourseDataInternal() {
        viewModelScope.launch {
            try {
                var courseStructure = interactor.getCourseStructure(courseId)
                val blocks = courseStructure.blockData

                val courseStatus = if (networkConnection.isOnline()) {
                    interactor.getCourseStatus(courseId)
                } else {
                    CourseComponentStatus("")
                }

                val courseDatesResult = if (networkConnection.isOnline()) {
                    interactor.getCourseDates(courseId)
                } else {
                    CourseDatesResult(
                        datesSection = linkedMapOf(),
                        courseBanner = CourseDatesBannerInfo(
                            missedDeadlines = false,
                            missedGatedContent = false,
                            verifiedUpgradeLink = "",
                            contentTypeGatingEnabled = false,
                            hasEnded = false
                        )
                    )
                }
                val datesBannerInfo = courseDatesResult.courseBanner

                checkIfCalendarOutOfDate(courseDatesResult.datesSection.values.flatten())

                setBlocks(blocks)
                courseSubSections.clear()
                courseSubSectionUnit.clear()
                courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                initDownloadModelsStatus()

                val courseSectionsState =
                    (_uiState.value as? CourseOutlineUIState.CourseData)?.courseSectionsState.orEmpty()

                _uiState.value = CourseOutlineUIState.CourseData(
                    courseStructure = courseStructure,
                    downloadedState = getDownloadModelsStatus(),
                    resumeComponent = getResumeBlock(blocks, courseStatus.lastVisitedBlockId),
                    courseSubSections = courseSubSections,
                    courseSectionsState = courseSectionsState,
                    subSectionsDownloadsCount = subSectionsDownloadsCount,
                    datesBannerInfo = datesBannerInfo,
                )
                courseNotifier.send(CourseLoading(false))
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
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
                    blocks.find { it.id == descendant }?.let { sequentialBlock ->
                        if (isCourseNestedListEnabled) {
                            courseSubSections.getOrPut(block.id) { mutableListOf() }
                                .add(sequentialBlock)
                            courseSubSectionUnit[sequentialBlock.id] =
                                sequentialBlock.getFirstDescendantBlock(blocks)
                            subSectionsDownloadsCount[sequentialBlock.id] =
                                sequentialBlock.getDownloadsCount(blocks)

                        } else {
                            resultBlocks.add(sequentialBlock)
                        }
                        addDownloadableChildrenForSequentialBlock(sequentialBlock)
                    }
                }
            }
        }
        return resultBlocks.toList()
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

    fun resetCourseDatesBanner(onResetDates: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                interactor.resetCourseDates(courseId = courseId)
                updateCourseData()
                courseNotifier.send(CourseDatesShifted)
                onResetDates(true)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_dates_shift_dates_unsuccessful_msg)))
                }
                onResetDates(false)
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

    fun resumeCourseTappedEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseOutlineUIState.CourseData) {
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
        if (currentState is CourseOutlineUIState.CourseData) {
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
        if (currentState is CourseOutlineUIState.CourseData) {
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
}
