package org.openedx.discovery.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryAnalyticsEvent
import org.openedx.discovery.presentation.DiscoveryAnalyticsKey
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class CourseDetailsViewModel(
    val courseId: String,
    private val config: Config,
    private val corePreferences: CorePreferences,
    private val networkConnection: NetworkConnection,
    private val interactor: DiscoveryInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscoveryNotifier,
    private val analytics: DiscoveryAnalytics,
    private val calendarSyncScheduler: CalendarSyncScheduler,
) : BaseViewModel() {
    val apiHostUrl get() = config.getApiHostURL()
    val isUserLoggedIn get() = corePreferences.user != null
    val isRegistrationEnabled: Boolean get() = config.isRegistrationEnabled()

    private val _uiState = MutableLiveData<CourseDetailsUIState>(CourseDetailsUIState.Loading)
    val uiState: LiveData<CourseDetailsUIState>
        get() = _uiState
    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private var course: Course? = null

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        getCourseDetail()
    }

    fun getCourseDetail() {
        _uiState.value = CourseDetailsUIState.Loading
        viewModelScope.launch {
            try {
                course = if (hasInternetConnection) {
                    interactor.getCourseDetails(courseId)
                } else {
                    interactor.getCourseDetailsFromCache(courseId)
                }
                course?.let {
                    _uiState.value = CourseDetailsUIState.CourseData(
                        course = it,
                        isUserLoggedIn = isUserLoggedIn
                    )
                } ?: run {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun enrollInACourse(id: String, title: String) {
        viewModelScope.launch {
            try {
                val courseData = _uiState.value
                if (courseData is CourseDetailsUIState.CourseData) {
                    courseEnrollClickedEvent(id, title)
                }
                interactor.enrollInACourse(id)
                val course = interactor.getCourseDetails(id)
                if (courseData is CourseDetailsUIState.CourseData) {
                    _uiState.value = courseData.copy(course = course)
                    courseEnrollSuccessEvent(id, title)
                    calendarSyncScheduler.requestImmediateSync(id)
                    notifier.send(CourseDashboardUpdate())
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun getCourseAboutBody(bgColor: ULong, textColor: ULong): String {
        val darkThemeStyle = "<style>\n" +
                "      body {\n" +
                "        background-color: #${getColorFromULong(bgColor)};\n" +
                "        color: #${getColorFromULong(textColor)};\n" +
                "      }\n" +
                "    </style>"
        val buff = StringBuffer().apply {
            if (bgColor != ULong.MIN_VALUE) append(darkThemeStyle)
            append("<body>")
            append("<div class=\"header\">")
            append(course?.overview ?: "")
            append("</div>")
            append("</body>")
        }
        return buff.toString()
    }

    private fun getColorFromULong(color: ULong): String {
        if (color == ULong.MIN_VALUE) return "black"
        return java.lang.Long.toHexString(color.toLong()).substring(
            startIndex = 2,
            endIndex = 8
        )
    }

    private fun courseEnrollClickedEvent(courseId: String, courseTitle: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED, courseId, courseTitle)
    }

    private fun courseEnrollSuccessEvent(courseId: String, courseTitle: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_ENROLL_SUCCESS, courseId, courseTitle)
    }

    private fun logEvent(
        event: DiscoveryAnalyticsEvent,
        courseId: String,
        courseTitle: String,
    ) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
                put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
                put(DiscoveryAnalyticsKey.COURSE_NAME.key, courseTitle)
                put(DiscoveryAnalyticsKey.CONVERSION.key, courseId)
            }
        )
    }
}
