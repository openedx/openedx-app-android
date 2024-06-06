package org.openedx.course.presentation.container

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncUIState
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.RefreshDates
import org.openedx.core.system.notifier.RefreshDiscussions
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CalendarSyncDialog
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseContainerViewModel(
    val courseId: String,
    var courseName: String,
    private var resumeBlockId: String,
    private val enrollmentMode: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val courseAnalytics: CourseAnalytics,
    private val imageProcessor: ImageProcessor,
    private val calendarSyncScheduler: CalendarSyncScheduler,
    val courseRouter: CourseRouter,
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

    private val _calendarSyncUIState = MutableStateFlow(
        CalendarSyncUIState(
            isCalendarSyncEnabled = isCalendarSyncEnabled(),
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
                        calendarSyncScheduler.requestImmediateSync(courseId)
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
                if (_dataReady.value == true && resumeBlockId.isNotEmpty()) {
                    delay(500L)
                    courseNotifier.send(CourseOpenBlock(resumeBlockId))
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
                    courseNotifier.send(RefreshDates)
                }
            }

            CourseContainerTab.DISCUSSIONS -> {
                viewModelScope.launch {
                    courseNotifier.send(RefreshDiscussions)
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
