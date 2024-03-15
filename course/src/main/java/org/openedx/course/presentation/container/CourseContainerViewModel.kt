package org.openedx.course.presentation.container

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.SingleEventLiveData
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CheckCalendarSyncEvent
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.utils.TimeUtils
import org.openedx.course.R
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalyticEvent
import org.openedx.course.presentation.CourseAnalyticKey
import org.openedx.course.presentation.CourseAnalyticValue
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.calendarsync.CalendarManager
import org.openedx.course.presentation.calendarsync.CalendarSyncDialogType
import org.openedx.course.presentation.calendarsync.CalendarSyncUIState
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseContainerViewModel(
    val courseId: String,
    var courseName: String,
    val enrollmentMode: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val calendarManager: CalendarManager,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val coursePreferences: CoursePreferences,
    private val courseAnalytics: CourseAnalytics,
) : BaseViewModel() {

    val isCourseTopTabBarEnabled get() = config.isCourseTopTabBarEnabled()

    private val _dataReady = MutableLiveData<Boolean?>()
    val dataReady: LiveData<Boolean?>
        get() = _dataReady

    private val _errorMessage = SingleEventLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _showProgress = MutableLiveData<Boolean>()
    val showProgress: LiveData<Boolean>
        get() = _showProgress

    private var _isSelfPaced: Boolean = true
    val isSelfPaced: Boolean
        get() = _isSelfPaced

    val calendarPermissions: Array<String>
        get() = calendarManager.permissions

    private val _calendarSyncUIState = MutableStateFlow(
        CalendarSyncUIState(
            isCalendarSyncEnabled = isCalendarSyncEnabled(),
            calendarTitle = calendarManager.getCourseCalendarTitle(courseName),
            courseDates = emptyList(),
            dialogType = CalendarSyncDialogType.NONE,
            checkForOutOfSync = AtomicReference(false),
            uiMessage = AtomicReference(""),
        )
    )
    val calendarSyncUIState: StateFlow<CalendarSyncUIState> =
        _calendarSyncUIState.asStateFlow()

    init {
        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseCompletionSet) {
                    updateData(false)
                }

                if (event is CreateCalendarSyncEvent) {
                    _calendarSyncUIState.update {
                        val dialogType = CalendarSyncDialogType.valueOf(event.dialogType)
                        it.copy(
                            courseDates = event.courseDates,
                            dialogType = dialogType,
                            checkForOutOfSync = AtomicReference(event.checkOutOfSync)
                        )
                    }
                }
            }
        }
    }

    fun preloadCourseStructure() {
        courseDashboardViewed()
        if (_dataReady.value != null) {
            return
        }

        _showProgress.value = true
        viewModelScope.launch {
            try {
                if (networkConnection.isOnline()) {
                    interactor.preloadCourseStructure(courseId)
                } else {
                    interactor.preloadCourseStructureFromCache(courseId)
                }
                val courseStructure = interactor.getCourseStructureFromCache()
                courseName = courseStructure.name
                _isSelfPaced = courseStructure.isSelfPaced
                _dataReady.value = courseStructure.start?.let { start ->
                    start < Date()
                }
            } catch (e: Exception) {
                if (e.isInternetError() || e is NoCachedDataException) {
                    _errorMessage.value =
                        resourceManager.getString(CoreR.string.core_error_no_connection)
                } else {
                    _errorMessage.value =
                        resourceManager.getString(CoreR.string.core_error_unknown_error)
                }
            }
            _showProgress.value = false
        }
    }

    fun updateData(withSwipeRefresh: Boolean) {
        _showProgress.value = true
        viewModelScope.launch {
            try {
                interactor.preloadCourseStructure(courseId)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _errorMessage.value =
                        resourceManager.getString(CoreR.string.core_error_no_connection)
                } else {
                    _errorMessage.value =
                        resourceManager.getString(CoreR.string.core_error_unknown_error)
                }
            }
            _showProgress.value = false
            notifier.send(CourseStructureUpdated(courseId, withSwipeRefresh))
        }
    }

    fun courseContainerTabClickedEvent(tab: CourseContainerTab) {
        when (tab) {
            CourseContainerTab.COURSE -> courseTabClickedEvent()
            CourseContainerTab.VIDEOS -> videoTabClickedEvent()
            CourseContainerTab.DISCUSSION -> discussionTabClickedEvent()
            CourseContainerTab.DATES -> datesTabClickedEvent()
            CourseContainerTab.HANDOUTS -> handoutsTabClickedEvent()
        }
    }

    fun setCalendarSyncDialogType(dialogType: CalendarSyncDialogType) {
        val currentState = _calendarSyncUIState.value
        if (currentState.dialogType != dialogType) {
            _calendarSyncUIState.value = currentState.copy(dialogType = dialogType)
        }
    }

    fun addOrUpdateEventsInCalendar(
        updatedEvent: Boolean,
    ) {
        setCalendarSyncDialogType(CalendarSyncDialogType.LOADING_DIALOG)

        val startSyncTime = TimeUtils.getCurrentTime()
        val calendarId = getCalendarId()

        if (calendarId == CalendarManager.CALENDAR_DOES_NOT_EXIST) {
            setUiMessage(R.string.course_snackbar_course_calendar_error)
            setCalendarSyncDialogType(CalendarSyncDialogType.NONE)

            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val courseDates = _calendarSyncUIState.value.courseDates
            if (courseDates.isNotEmpty()) {
                courseDates.forEach { courseDateBlock ->
                    calendarManager.addEventsIntoCalendar(
                        calendarId = calendarId,
                        courseId = courseId,
                        courseName = courseName,
                        courseDateBlock = courseDateBlock
                    )
                }
            }
            val elapsedSyncTime = TimeUtils.getCurrentTime() - startSyncTime
            val delayRemaining = maxOf(0, 1000 - elapsedSyncTime)

            // Ensure minimum 1s delay to prevent flicker for rapid event creation
            if (delayRemaining > 0) {
                delay(delayRemaining)
            }

            setCalendarSyncDialogType(CalendarSyncDialogType.NONE)
            updateCalendarSyncState()

            if (updatedEvent) {
                logCalendarUpdateDatesSuccess()
                setUiMessage(R.string.course_snackbar_course_calendar_updated)
            } else if (coursePreferences.isCalendarSyncEventsDialogShown(courseName)) {
                logCalendarAddDatesSuccess()
                setUiMessage(R.string.course_snackbar_course_calendar_added)
            } else {
                logCalendarAddDatesSuccess()
                coursePreferences.setCalendarSyncEventsDialogShown(courseName)
                setCalendarSyncDialogType(CalendarSyncDialogType.EVENTS_DIALOG)
            }
        }
    }

    private fun updateCalendarSyncState() {
        viewModelScope.launch {
            val isCalendarSynced = calendarManager.isCalendarExists(
                calendarTitle = _calendarSyncUIState.value.calendarTitle
            )
            notifier.send(CheckCalendarSyncEvent(isSynced = isCalendarSynced))
        }
    }

    fun checkIfCalendarOutOfDate() {
        val courseDates = _calendarSyncUIState.value.courseDates
        if (courseDates.isNotEmpty()) {
            _calendarSyncUIState.value.checkForOutOfSync.set(false)
            val outdatedCalendarId = calendarManager.isCalendarOutOfDate(
                calendarTitle = _calendarSyncUIState.value.calendarTitle,
                courseDateBlocks = courseDates
            )
            if (outdatedCalendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
                setCalendarSyncDialogType(CalendarSyncDialogType.OUT_OF_SYNC_DIALOG)
            }
        }
    }

    fun deleteCourseCalendar() {
        if (calendarManager.hasPermissions()) {
            viewModelScope.launch(Dispatchers.IO) {
                val calendarId = getCalendarId()
                if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
                    calendarManager.deleteCalendar(
                        calendarId = calendarId,
                    )
                }
                updateCalendarSyncState()
            }
            logCalendarRemoveDatesSuccess()
            setUiMessage(R.string.course_snackbar_course_calendar_removed)
        }
    }

    fun openCalendarApp() {
        calendarManager.openCalendarApp()
    }

    private fun setUiMessage(@StringRes stringResId: Int) {
        _calendarSyncUIState.update {
            it.copy(uiMessage = AtomicReference(resourceManager.getString(stringResId)))
        }
    }

    private fun getCalendarId(): Long {
        return calendarManager.createOrUpdateCalendar(
            calendarTitle = _calendarSyncUIState.value.calendarTitle
        )
    }

    private fun isCalendarSyncEnabled(): Boolean {
        val calendarSync = corePreferences.appConfig.courseDatesCalendarSync
        return calendarSync.isEnabled && ((calendarSync.isSelfPacedEnabled && isSelfPaced) ||
                (calendarSync.isInstructorPacedEnabled && !isSelfPaced))
    }

    private fun courseDashboardViewed(){
        logCourseContainerEvent(CourseAnalyticEvent.DASHBOARD, CourseAnalyticValue.DASHBOARD)
    }

    private fun courseTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticEvent.HOME_TAB, CourseAnalyticValue.HOME_TAB)
    }

    private fun videoTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticEvent.VIDEOS_TAB, CourseAnalyticValue.VIDEOS_TAB)
    }

    private fun discussionTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticEvent.DISCUSSION_TAB, CourseAnalyticValue.DISCUSSION_TAB)
    }

    private fun datesTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticEvent.DATES_TAB, CourseAnalyticValue.DATES_TAB)
    }

    private fun handoutsTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticEvent.HANDOUTS_TAB, CourseAnalyticValue.HANDOUTS_TAB)
    }

    private fun logCourseContainerEvent(event: CourseAnalyticEvent, biValue: CourseAnalyticValue) {
        courseAnalytics.logEvent(
            event = event.event,
            params = buildMap {
                put(CourseAnalyticKey.NAME.key, biValue.biValue)
                put(CourseAnalyticKey.COURSE_ID.key, courseId)
            }
        )
    }

    fun logCalendarPermissionAccess(isAllowed: Boolean) {
        if (isAllowed) {
            logCalendarSyncEvent(
                CourseAnalyticEvent.DATES_CALENDAR_ACCESS_ALLOWED,
                CourseAnalyticValue.DATES_CALENDAR_ACCESS_ALLOWED
            )
        } else {
            logCalendarSyncEvent(
                CourseAnalyticEvent.DATES_CALENDAR_ACCESS_DONT_ALLOW,
                CourseAnalyticValue.DATES_CALENDAR_ACCESS_DONT_ALLOW
            )
        }
    }

    fun logCalendarAddDates() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_ADD_DATES,
            CourseAnalyticValue.DATES_CALENDAR_ADD_DATES
        )
    }

    fun logCalendarAddCancelled() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_ADD_CANCELLED,
            CourseAnalyticValue.DATES_CALENDAR_ADD_CANCELLED
        )
    }

    fun logCalendarRemoveDates() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_REMOVE_DATES,
            CourseAnalyticValue.DATES_CALENDAR_REMOVE_DATES
        )
    }

    fun logCalendarRemoveCancelled() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_REMOVE_CANCELLED,
            CourseAnalyticValue.DATES_CALENDAR_REMOVE_CANCELLED
        )
    }

    fun logCalendarAddedConfirmation() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_ADD_CONFIRMATION,
            CourseAnalyticValue.DATES_CALENDAR_ADD_CONFIRMATION
        )
    }

    fun logCalendarViewEvents() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_VIEW_EVENTS,
            CourseAnalyticValue.DATES_CALENDAR_VIEW_EVENTS
        )
    }

    fun logCalendarSyncUpdateDates() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_SYNC_UPDATE_DATES,
            CourseAnalyticValue.DATES_CALENDAR_SYNC_UPDATE_DATES
        )
    }

    fun logCalendarSyncRemoveCalendar() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_SYNC_REMOVE_CALENDAR,
            CourseAnalyticValue.DATES_CALENDAR_SYNC_REMOVE_CALENDAR
        )
    }

    private fun logCalendarAddDatesSuccess() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_ADD_DATES_SUCCESS,
            CourseAnalyticValue.DATES_CALENDAR_ADD_DATES_SUCCESS
        )
    }

    private fun logCalendarRemoveDatesSuccess() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_REMOVE_DATES_SUCCESS,
            CourseAnalyticValue.DATES_CALENDAR_REMOVE_DATES_SUCCESS
        )
    }

    private fun logCalendarUpdateDatesSuccess() {
        logCalendarSyncEvent(
            CourseAnalyticEvent.DATES_CALENDAR_UPDATE_DATES_SUCCESS,
            CourseAnalyticValue.DATES_CALENDAR_UPDATE_DATES_SUCCESS
        )
    }

    private fun logCalendarSyncEvent(
        event: CourseAnalyticEvent,
        value: CourseAnalyticValue,
        param: Map<String, Any> = emptyMap(),
    ) {
        courseAnalytics.logEvent(
            event = event.event,
            params = buildMap {
                put(CourseAnalyticKey.NAME.key, value.biValue)
                put(CourseAnalyticKey.COURSE_ID.key, courseId)
                put(CourseAnalyticKey.ENROLLMENT_MODE.key, enrollmentMode)
                put(CourseAnalyticKey.PACING.key, isSelfPaced)
                putAll(param)
            }
        )
    }
}
