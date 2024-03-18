package org.openedx.course.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Course
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalyticKey
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent

class CourseDetailsViewModel(
    val courseId: String,
    private val config: Config,
    private val corePreferences: CorePreferences,
    private val networkConnection: NetworkConnection,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
) : BaseViewModel() {
    val apiHostUrl get() = config.getApiHostURL()
    val isUserLoggedIn get() = corePreferences.user != null

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
        return java.lang.Long.toHexString(color.toLong()).substring(2, 8)
    }

    private fun courseEnrollClickedEvent(courseId: String, courseTitle: String) {
        logEvent(CourseAnalyticsEvent.COURSE_ENROLL_CLICKED, courseId, courseTitle)
    }

    private fun courseEnrollSuccessEvent(courseId: String, courseTitle: String) {
        logEvent(CourseAnalyticsEvent.COURSE_ENROLL_SUCCESS, courseId, courseTitle)
    }

    fun viewCourseClickedEvent(courseId: String, courseTitle: String) {
        logEvent(CourseAnalyticsEvent.COURSE_INFO, courseId, courseTitle)
    }

    private fun logEvent(
        event: CourseAnalyticsEvent,
        courseId: String, courseTitle: String,
    ) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(CourseAnalyticKey.NAME.key, event.biValue)
                put(CourseAnalyticKey.COURSE_ID.key, courseId)
                put(CourseAnalyticKey.COURSE_NAME.key, courseTitle)
                put(CourseAnalyticKey.CONVERSION.key, courseId)
            }
        )
    }
}
