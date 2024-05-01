package org.openedx.courses.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.dashboard.domain.CourseStatusFilter
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardRouter

class AllEnrolledCoursesViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val analytics: DashboardAnalytics,
    val dashboardRouter: DashboardRouter
) : BaseViewModel() {

    private val coursesList = mutableListOf<EnrolledCourse>()
    private var page = 1
    private var isLoading = false

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableStateFlow<AllEnrolledCoursesUIState>(AllEnrolledCoursesUIState.Loading)
    val uiState: StateFlow<AllEnrolledCoursesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _updating = MutableStateFlow<Boolean>(false)
    val updating: StateFlow<Boolean>
        get() = _updating.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val _canLoadMore = MutableStateFlow<Boolean>(false)
    val canLoadMore: StateFlow<Boolean>
        get() = _canLoadMore.asStateFlow()

    private val currentFilter: MutableStateFlow<CourseStatusFilter> = MutableStateFlow(CourseStatusFilter.ALL)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            discoveryNotifier.notifier.collect {
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }
    }

    init {
        getCourses(currentFilter.value)
    }

    fun getCourses(courseStatusFilter: CourseStatusFilter? = null) {
        _uiState.value = AllEnrolledCoursesUIState.Loading
        coursesList.clear()
        internalLoadingCourses(courseStatusFilter ?: currentFilter.value)
    }

    fun updateCourses() {
        viewModelScope.launch {
            try {
                _updating.value = true
                isLoading = true
                page = 1
                val response = interactor.getAllUserCourses(page, currentFilter.value)
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                coursesList.clear()
                coursesList.addAll(response.courses)
                if (coursesList.isEmpty()) {
                    _uiState.value = AllEnrolledCoursesUIState.Empty
                } else {
                    _uiState.value = AllEnrolledCoursesUIState.Courses(ArrayList(coursesList))
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            }
            _updating.value = false
            isLoading = false
        }
    }

    private fun internalLoadingCourses(courseStatusFilter: CourseStatusFilter? = null) {
        if (courseStatusFilter != null) {
            page = 1
            currentFilter.value = courseStatusFilter
        }
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getAllUserCourses(page, currentFilter.value)
                } else {
                    null
                }
                if (response != null) {
                    if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                        _canLoadMore.value = true
                        page++
                    } else {
                        _canLoadMore.value = false
                        page = -1
                    }
                    coursesList.addAll(response.courses)
                } else {
                    val cachedList = interactor.getEnrolledCoursesFromCache()
                    _canLoadMore.value = false
                    page = -1
                    coursesList.addAll(cachedList)
                }
                if (coursesList.isEmpty()) {
                    _uiState.value = AllEnrolledCoursesUIState.Empty
                } else {
                    _uiState.value = AllEnrolledCoursesUIState.Courses(ArrayList(coursesList))
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            }
            _updating.value = false
            isLoading = false
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            internalLoadingCourses()
        }
    }

    fun dashboardCourseClickedEvent(courseId: String, courseName: String) {
        analytics.dashboardCourseClickedEvent(courseId, courseName)
    }

}

interface AllEnrolledCoursesAction {
    object Reload : AllEnrolledCoursesAction
    object SwipeRefresh : AllEnrolledCoursesAction
    object EndOfPage : AllEnrolledCoursesAction
    object Back : AllEnrolledCoursesAction
    object Search : AllEnrolledCoursesAction
    data class OpenCourse(val enrolledCourse: EnrolledCourse) : AllEnrolledCoursesAction
    data class FilterChange(val courseStatusFilter: CourseStatusFilter?) : AllEnrolledCoursesAction
}
