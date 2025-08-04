package org.openedx.course.presentation.container

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.createBitmap
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseAccessError
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.extension.isFalse
import org.openedx.core.extension.isTrue
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncUIState
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseStructureGot
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.RefreshDates
import org.openedx.core.system.notifier.RefreshDiscussions
import org.openedx.core.system.notifier.RefreshProgress
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.course.DatesShiftedSnackBar
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CalendarSyncDialog
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.utils.ImageProcessor
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.extension.toImageLink
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseContainerViewModel(
    val courseId: String,
    var courseName: String,
    private var resumeBlockId: String,
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

    private val _courseAccessStatus = MutableLiveData<CourseAccessError>()
    val courseAccessStatus: LiveData<CourseAccessError>
        get() = _courseAccessStatus

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

    private var _courseDetails: CourseEnrollmentDetails? = null
    val courseDetails: CourseEnrollmentDetails?
        get() = _courseDetails

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

    private var _courseImage = MutableStateFlow(createBitmap(1, 1))
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
                        // Skip out-of-sync check if any calendar dialog is visible
                        if (event.checkOutOfSync && _calendarSyncUIState.value.isDialogVisible) {
                            return@collect
                        }
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

    fun fetchCourseDetails() {
        courseDashboardViewed()

        // If data is already loaded, do nothing
        if (_dataReady.value != null) return

        _showProgress.value = true

        viewModelScope.launch {
            val courseStructureFlow = interactor.getCourseStructureFlow(courseId)
                .catch { e ->
                    handleFetchError(e)
                    emit(null)
                }
            val courseDetailsFlow = interactor.getEnrollmentDetailsFlow(courseId)
                .catch { emit(null) }
            courseStructureFlow.combine(courseDetailsFlow) { courseStructure, courseEnrollmentDetails ->
                courseStructure to courseEnrollmentDetails
            }.catch { e ->
                handleFetchError(e)
            }.collect { (courseStructure, courseEnrollmentDetails) ->
                when {
                    courseEnrollmentDetails != null -> handleCourseEnrollment(courseEnrollmentDetails)
                    courseStructure != null -> handleCourseStructureOnly(courseStructure)
                    else -> _courseAccessStatus.value = CourseAccessError.UNKNOWN
                }
                courseNotifier.send(CourseStructureGot(courseId))
            }
        }
    }

    /**
     * Handles the scenario where [CourseEnrollmentDetails] is successfully fetched.
     */
    private fun handleCourseEnrollment(courseDetails: CourseEnrollmentDetails) {
        _courseDetails = courseDetails
        courseName = courseDetails.courseInfoOverview.name
        loadCourseImage(courseDetails.courseInfoOverview.media?.image?.large)

        if (courseDetails.hasAccess.isFalse()) {
            _dataReady.value = false
            _courseAccessStatus.value = when {
                courseDetails.isAuditAccessExpired -> CourseAccessError.AUDIT_EXPIRED_NOT_UPGRADABLE
                courseDetails.courseInfoOverview.isStarted.not() -> CourseAccessError.NOT_YET_STARTED
                else -> CourseAccessError.UNKNOWN
            }
        } else {
            _courseAccessStatus.value = CourseAccessError.NONE
            _isNavigationEnabled.value = true
            _calendarSyncUIState.update { state ->
                state.copy(isCalendarSyncEnabled = isCalendarSyncEnabled())
            }
            if (resumeBlockId.isNotEmpty()) {
                // Small delay before sending block open event
                viewModelScope.launch {
                    delay(500L)
                    courseNotifier.send(CourseOpenBlock(resumeBlockId))
                }
            }
            _dataReady.value = true
        }
    }

    /**
     * Handles the scenario where we only have [CourseStructure] but no enrollment details.
     */
    private fun handleCourseStructureOnly(courseStructure: CourseStructure) {
        loadCourseImage(courseStructure.media?.image?.large)
        _courseAccessStatus.value = CourseAccessError.NONE
        _isNavigationEnabled.value = true
        _calendarSyncUIState.update { state ->
            state.copy(isCalendarSyncEnabled = isCalendarSyncEnabled())
        }
        if (resumeBlockId.isNotEmpty()) {
            viewModelScope.launch {
                delay(500L)
                courseNotifier.send(CourseOpenBlock(resumeBlockId))
            }
        }
        _dataReady.value = true
    }

    private fun handleFetchError(e: Throwable) {
        e.printStackTrace()
        if (isNetworkRelatedError(e)) {
            _errorMessage.value = resourceManager.getString(CoreR.string.core_error_no_connection)
        } else {
            _courseAccessStatus.value = CourseAccessError.UNKNOWN
        }
        _showProgress.value = false
    }

    private fun isNetworkRelatedError(e: Throwable): Boolean {
        return e.isInternetError() || e is NoCachedDataException
    }

    private fun loadCourseImage(imageUrl: String?) {
        imageProcessor.loadImage(
            imageUrl = imageUrl?.toImageLink(config.getApiHostURL()) ?: "",
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

            CourseContainerTab.OFFLINE -> {
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

            CourseContainerTab.PROGRESS -> {
                viewModelScope.launch {
                    courseNotifier.send(RefreshProgress)
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
            } catch (_: Exception) {
                _errorMessage.value =
                    resourceManager.getString(CoreR.string.core_error_unknown_error)
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
            CourseContainerTab.PROGRESS -> progressTabClickedEvent()
            CourseContainerTab.MORE -> moreTabClickedEvent()
            CourseContainerTab.OFFLINE -> {}
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
        return calendarSync.isEnabled && (
                isSelfPacedCalendarSyncEnabled(calendarSync) ||
                        isInstructorPacedCalendarSyncEnabled(calendarSync)
                )
    }

    private fun isSelfPacedCalendarSyncEnabled(calendarSync: CourseDatesCalendarSync): Boolean {
        return calendarSync.isSelfPacedEnabled && _courseDetails?.courseInfoOverview?.isSelfPaced.isTrue()
    }

    private fun isInstructorPacedCalendarSyncEnabled(calendarSync: CourseDatesCalendarSync): Boolean {
        return calendarSync.isInstructorPacedEnabled && _courseDetails?.courseInfoOverview?.isSelfPaced.isFalse()
    }

    private fun courseDashboardViewed() {
        logCourseContainerEvent(CourseAnalyticsEvent.DASHBOARD)
        courseTabClickedEvent()
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

    private fun progressTabClickedEvent() {
        logCourseContainerEvent(CourseAnalyticsEvent.PROGRESS_TAB)
    }

    private fun logCourseContainerEvent(event: CourseAnalyticsEvent) {
        courseAnalytics.logScreenEvent(
            screenName = event.eventName,
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
                put(
                    CourseAnalyticsKey.ENROLLMENT_MODE.key,
                    _courseDetails?.enrollmentDetails?.mode ?: ""
                )
                put(
                    CourseAnalyticsKey.PACING.key,
                    if (_courseDetails?.courseInfoOverview?.isSelfPaced.isTrue()) {
                        CourseAnalyticsKey.SELF_PACED.key
                    } else {
                        CourseAnalyticsKey.INSTRUCTOR_PACED.key
                    }
                )
                putAll(param)
            }
        )
    }
}
