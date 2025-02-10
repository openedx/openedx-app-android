package org.openedx.course.presentation.dates

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.CalendarRouter
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseBannerType
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.extension.getSequentialBlocks
import org.openedx.core.extension.getVerticalBlocks
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.RefreshDates
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.core.R as CoreR

class CourseDatesViewModel(
    val courseId: String,
    private val enrollmentMode: String,
    private val courseNotifier: CourseNotifier,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val courseAnalytics: CourseAnalytics,
    private val config: Config,
    private val calendarInteractor: CalendarInteractor,
    private val calendarNotifier: CalendarNotifier,
    private val corePreferences: CorePreferences,
    val courseRouter: CourseRouter,
    val calendarRouter: CalendarRouter
) : BaseViewModel() {

    var isSelfPaced = true
    var useRelativeDates = corePreferences.isRelativeDatesEnabled

    private val _uiState = MutableStateFlow<CourseDatesUIState>(CourseDatesUIState.Loading)
    val uiState: StateFlow<CourseDatesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private var courseBannerType: CourseBannerType = CourseBannerType.BLANK
    private var courseStructure: CourseStructure? = null

    val isCourseExpandableSectionsEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is RefreshDates -> {
                        loadingCourseDatesInternal()
                    }
                }
            }
        }
        viewModelScope.launch {
            calendarNotifier.notifier.collect {
                (_uiState.value as? DatesUIState.Dates)?.let { currentUiState ->
                    val courseDates = currentUiState.courseDatesResult.datesSection.values.flatten()
                    _uiState.update {
                        (it as CourseDatesUIState.CourseDates).copy(calendarSyncState = getCalendarState(courseDates))
                    }
                }
            }
        }

        loadingCourseDatesInternal()
    }

    private fun loadingCourseDatesInternal() {
        viewModelScope.launch {
            try {
                courseStructure = interactor.getCourseStructure(courseId = courseId)
                isSelfPaced = courseStructure?.isSelfPaced ?: false
                val datesResponse = interactor.getCourseDates(courseId = courseId)
                if (datesResponse.datesSection.isEmpty()) {
                    _uiState.value = CourseDatesUIState.Error
                } else {
                    val courseDates = datesResponse.datesSection.values.flatten()
                    val calendarState = getCalendarState(courseDates)
                    _uiState.value = CourseDatesUIState.CourseDates(datesResponse, calendarState)
                    courseBannerType = datesResponse.courseBanner.bannerType
                    checkIfCalendarOutOfDate()
                }
            } catch (e: Exception) {
                _uiState.value = CourseDatesUIState.Error
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_error_no_connection))
                    )
                }
            } finally {
                courseNotifier.send(CourseLoading(false))
            }
        }
    }

    fun resetCourseDatesBanner(onResetDates: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                interactor.resetCourseDates(courseId = courseId)
                loadingCourseDatesInternal()
                courseNotifier.send(CourseDatesShifted)
                onResetDates(true)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_error_no_connection))
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

    fun getVerticalBlock(blockId: String): Block? {
        return try {
            courseStructure?.blockData?.getVerticalBlocks()
                ?.find { it.descendants.contains(blockId) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSequentialBlock(blockId: String): Block? {
        return try {
            courseStructure?.blockData?.getSequentialBlocks()
                ?.find { it.descendants.contains(blockId) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun checkIfCalendarOutOfDate() {
        val value = _uiState.value
        if (value is CourseDatesUIState.CourseDates) {
            viewModelScope.launch {
                courseNotifier.send(
                    CreateCalendarSyncEvent(
                        courseDates = value.courseDatesResult.datesSection.values.flatten(),
                        dialogType = CalendarSyncDialogType.NONE.name,
                        checkOutOfSync = true,
                    )
                )
            }
        }
    }

    private suspend fun getCalendarState(courseDates: List<CourseDateBlock>): CalendarSyncState {
        val courseCalendarState = calendarInteractor.getCourseCalendarStateByIdFromCache(courseId)
        return when {
            courseCalendarState?.isCourseSyncEnabled != true -> CalendarSyncState.OFFLINE
            !isCourseCalendarUpToDate(courseDates) -> CalendarSyncState.SYNC_FAILED
            else -> CalendarSyncState.SYNCED
        }
    }

    private suspend fun isCourseCalendarUpToDate(courseDateBlocks: List<CourseDateBlock>): Boolean {
        val oldChecksum = getCourseCalendarStateChecksum()
        val newChecksum = getCourseChecksum(courseDateBlocks)
        return newChecksum == oldChecksum
    }

    private fun getCourseChecksum(courseDateBlocks: List<CourseDateBlock>): Int {
        return courseDateBlocks.sumOf { it.hashCode() }
    }

    private suspend fun getCourseCalendarStateChecksum(): Int? {
        return calendarInteractor.getCourseCalendarStateByIdFromCache(courseId)?.checksum
    }

    fun logPlsBannerViewed() {
        logPLSBannerEvent(CourseAnalyticsEvent.PLS_BANNER_VIEWED)
    }

    fun logPlsShiftButtonClicked() {
        logPLSBannerEvent(CourseAnalyticsEvent.PLS_SHIFT_BUTTON_CLICKED)
    }

    fun logPlsShiftDates(isSuccess: Boolean) {
        logPLSBannerEvent(CourseAnalyticsEvent.PLS_SHIFT_DATES_SUCCESS, isSuccess)
    }

    fun logCourseComponentTapped(isSupported: Boolean, block: CourseDateBlock) {
        val params = buildMap<String, Any> {
            put(CourseAnalyticsKey.BLOCK_ID.key, block.blockId)
            put(CourseAnalyticsKey.BLOCK_TYPE.key, block.dateType)
            put(CourseAnalyticsKey.LINK.key, block.link)
            put(CourseAnalyticsKey.SUPPORTED.key, isSupported)
        }

        logDatesEvent(CourseAnalyticsEvent.DATES_COURSE_COMPONENT_CLICKED, params)
    }

    private fun logDatesEvent(
        event: CourseAnalyticsEvent,
        param: Map<String, Any> = emptyMap(),
    ) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.ENROLLMENT_MODE.key, enrollmentMode)
                put(
                    CourseAnalyticsKey.PACING.key,
                    if (isSelfPaced) {
                        CourseAnalyticsKey.SELF_PACED.key
                    } else {
                        CourseAnalyticsKey.INSTRUCTOR_PACED.key
                    }
                )
                putAll(param)
            }
        )
    }

    private fun logPLSBannerEvent(
        event: CourseAnalyticsEvent,
        isSuccess: Boolean? = null,
    ) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.CATEGORY.key, CourseAnalyticsKey.COURSE_DATES.key)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.ENROLLMENT_MODE.key, enrollmentMode)
                put(CourseAnalyticsKey.BANNER_TYPE.key, courseBannerType.name)
                put(CourseAnalyticsKey.SCREEN_NAME.key, CourseAnalyticsKey.COURSE_DATES.key)
                isSuccess?.let { put(CourseAnalyticsKey.SUCCESS.key, it) }
            }
        )
    }
}
