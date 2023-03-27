package com.raccoongang.course.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseDashboardUpdate
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class CourseDetailsViewModel(
    private val courseId: String,
    private val networkConnection: NetworkConnection,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier
) : BaseViewModel() {

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
                supervisorScope {
                    val courseJob = async {
                        if (networkConnection.isOnline()) {
                            interactor.getCourseDetails(courseId)
                        } else {
                            interactor.getCourseDetailsFromCache(courseId)
                        }
                    }
                    val enrolledCourse = async {
                        if (networkConnection.isOnline()) {
                            interactor.getEnrolledCourseById(courseId)
                        } else {
                            interactor.getEnrolledCourseFromCacheById(courseId)
                        }
                    }
                    val data = awaitAll(courseJob, enrolledCourse)
                    course = data[0] as Course
                    _uiState.value = CourseDetailsUIState.CourseData(
                        course = course!!,
                        data[1] as EnrolledCourse?
                    )
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

    fun enrollInACourse(id: String) {
        viewModelScope.launch {
            try {
                interactor.enrollInACourse(id)
                val enrolledCourse = interactor.getEnrolledCourseById(id)
                val course = interactor.getCourseDetails(id)
                val courseData = _uiState.value
                if (courseData is CourseDetailsUIState.CourseData) {
                    _uiState.value = courseData.copy(course = course, enrolledCourse = enrolledCourse)
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
}