package org.openedx.course.presentation.container

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.ImageProcessor
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.extension.isInternetError
import org.openedx.core.presentation.course.CourseContainerTab
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CheckCalendarSyncEvent
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseRefresh
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.utils.TimeUtils
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.R
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CalendarSyncDialog
import org.openedx.course.presentation.CalendarSyncSnackbar
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.calendarsync.CalendarManager
import org.openedx.course.presentation.calendarsync.CalendarSyncDialogType
import org.openedx.course.presentation.calendarsync.CalendarSyncUIState
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseContainerViewModel(
    val courseId: String,
    var courseName: String,
    private val enrollmentMode: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val calendarManager: CalendarManager,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val discoveryNotifier: DiscoveryNotifier,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val coursePreferences: CoursePreferences,
    private val courseAnalytics: CourseAnalytics,
    private val imageProcessor: ImageProcessor,
    val courseRouter: CourseRouter
) : BaseViewModel() {

    private val _dataReady = MutableLiveData<Boolean?>()
    val dataReady: LiveData<Boolean?>
        get() = _dataReady

    private val _errorMessage = SingleEventLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _showProgress = MutableStateFlow(true)
    val showProgress: StateFlow<Boolean> =
        _showProgress.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> =
        _refreshing.asStateFlow()

    private val _isNavigationEnabled = MutableStateFlow(false)
    val isNavigationEnabled: StateFlow<Boolean> =
        _isNavigationEnabled.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private var _isSelfPaced: Boolean = true
    val isSelfPaced: Boolean
        get() = _isSelfPaced

    private var _organization: String = ""
    val organization: String
        get() = _organization

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

    private var _courseImage = MutableStateFlow(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    val courseImage: StateFlow<Bitmap> = _courseImage.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseCompletionSet -> {
                        updateData()
                    }

                    is CreateCalendarSyncEvent -> {
                        _calendarSyncUIState.update {
                            val dialogType = CalendarSyncDialogType.valueOf(event.dialogType)
                            it.copy(
                                courseDates = event.courseDates,
                                dialogType = dialogType,
                                checkForOutOfSync = AtomicReference(event.checkOutOfSync)
                            )
                        }
                    }

                    is CourseDatesShifted -> {
                        _uiMessage.emit(DatesShiftedSnackBar())
                    }

                    is CourseLoading -> {
                        _showProgress.value = event.isLoading
                        if (!event.isLoading) {
                            _refreshing.value = false
                        }
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
                val courseStructure = interactor.getCourseStructure(courseId)
                courseName = courseStructure.name
                _organization = courseStructure.org
                _isSelfPaced = courseStructure.isSelfPaced
                loadCourseImage(courseStructure.media?.image?.large)
                _dataReady.value = courseStructure.start?.let { start ->
                    val isReady = start < Date()
                    if (isReady) {
                        _isNavigationEnabled.value = true
                    }
                    isReady
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
        }
    }

    private fun loadCourseImage(imageUrl: String?) {
        imageProcessor.loadImage(
            imageUrl = config.getApiHostURL() + imageUrl,
            defaultImage = CoreR.drawable.core_no_image_course,
            onComplete = { drawable ->
                val bitmap = (drawable as BitmapDrawable).bitmap.apply {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        imageProcessor.applyBlur(this@apply, 10f)
                    }
                }
                viewModelScope.launch {
                    _courseImage.emit(bitmap)
                }
            }
        )
    }

    fun onRefresh(courseContainerTab: CourseContainerTab) {
        _refreshing.value = true
        when (courseContainerTab) {
            CourseContainerTab.HOME -> {
                updateData()
            }

            CourseContainerTab.VIDEOS -> {
                updateData()
            }

            CourseContainerTab.DATES -> {
                viewModelScope.launch {
                    courseNotifier.send(CourseRefresh(courseContainerTab))
                }
            }

            CourseContainerTab.DISCUSSIONS -> {
                viewModelScope.launch {
                    courseNotifier.send(CourseRefresh(courseContainerTab))
                }
            }

            else -> {
                _refreshing.value = false
            }
        }
    }

    fun updateData() {
        viewModelScope.launch {
            try {
                interactor.getCourseStructure(courseId, isNeedRefresh = true)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _errorMessage.value =
                        resourceManager.getString(CoreR.string.core_error_no_connection)
                } else {
                    _errorMessage.value =
                        resourceManager.getString(CoreR.string.core_error_unknown_error)
                }
            }
            _refreshing.value = false
            courseNotifier.send(CourseStructureUpdated(courseId))
        }
    }

    fun updateEnrolledCourses() {
        viewModelScope.launch {
            discoveryNotifier.send(CourseDashboardUpdate())
        }
    }

    fun courseContainerTabClickedEvent(index: Int) {
        when (CourseContainerTab.entries[index]) {
            CourseContainerTab.HOME -> courseTabClickedEvent()
            CourseContainerTab.VIDEOS -> videoTabClickedEvent()
            CourseContainerTab.DISCUSSIONS -> discussionTabClickedEvent()
            CourseContainerTab.DATES -> datesTabClickedEvent()
            CourseContainerTab.MORE -> moreTabClickedEvent()
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
                logCalendarSyncSnackbar(CalendarSyncSnackbar.UPDATED)
                setUiMessage(R.string.course_snackbar_course_calendar_updated)
            } else if (coursePreferences.isCalendarSyncEventsDialogShown(courseName)) {
                logCalendarSyncSnackbar(CalendarSyncSnackbar.ADDED)
                setUiMessage(R.string.course_snackbar_course_calendar_added)
            } else {
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
            courseNotifier.send(CheckCalendarSyncEvent(isSynced = isCalendarSynced))
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
            logCalendarSyncSnackbar(CalendarSyncSnackbar.REMOVED)
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

    private fun courseDashboardViewed() {
        logCourseContainerEvent(CourseAnalyticsEvent.DASHBOARD)
    }

    private fun courseTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.HOME_TAB)
    }

    private fun videoTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.VIDEOS_TAB)
    }

    private fun discussionTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.DISCUSSION_TAB)
    }

    private fun datesTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.DATES_TAB)
    }

    private fun moreTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.MORE_TAB)
    }

    private fun logCourseContainerEvent(event: CourseAnalyticsEvent) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseName)
            }
        )
    }

    fun logCalendarPermissionAccess(isAllowed: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.PERMISSION.getBuildMap(isAllowed)
        )
    }

    fun logCalendarAddDates(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.ADD.getBuildMap(action)
        )
    }

    fun logCalendarRemoveDates(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.REMOVE.getBuildMap(action)
        )
    }

    fun logCalendarSyncedConfirmation(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.CONFIRMED.getBuildMap(action)
        )
    }

    fun logCalendarSyncUpdate(action: Boolean) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_DIALOG_ACTION,
            CalendarSyncDialog.UPDATE.getBuildMap(action)
        )
    }

    private fun logCalendarSyncSnackbar(snackbar: CalendarSyncSnackbar) {
        logCalendarSyncEvent(
            CourseAnalyticsEvent.DATES_CALENDAR_SYNC_SNACKBAR,
            snackbar.getBuildMap()
        )
    }

    private fun logCalendarSyncEvent(
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
                    if (isSelfPaced) CourseAnalyticsKey.SELF_PACED.key
                    else CourseAnalyticsKey.INSTRUCTOR_PACED.key
                )
                putAll(param)
            }
        )
    }
}
