package org.openedx.courses.presentation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.dashboard.domain.CourseStatusFilter
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class AllEnrolledCoursesViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val analytics: DashboardAnalytics,
    private val dashboardRouter: DashboardRouter
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()
    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val coursesList = mutableListOf<EnrolledCourse>()
    private var page = 1
    private var isLoading = false

    private val _uiState = MutableStateFlow(AllEnrolledCoursesUIState())
    val uiState: StateFlow<AllEnrolledCoursesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val currentFilter: MutableStateFlow<CourseStatusFilter> = MutableStateFlow(CourseStatusFilter.ALL)

    private var job: Job? = null

    init {
        collectDiscoveryNotifier()
        loadInitialCourses()
    }

    private fun loadInitialCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(showProgress = true) }
            val cachedList = interactor.getEnrolledCoursesFromCache()
            if (cachedList.isNotEmpty()) {
                _uiState.update { it.copy(courses = cachedList.toList(), showProgress = false) }
            }
            getCourses(showLoadingProgress = false)
        }
    }

    fun getCourses(courseStatusFilter: CourseStatusFilter? = null, showLoadingProgress: Boolean = true) {
        if (showLoadingProgress) {
            _uiState.update { it.copy(showProgress = true) }
        }
        coursesList.clear()
        internalLoadingCourses(courseStatusFilter ?: currentFilter.value)
    }

    fun updateCourses() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(refreshing = true) }
                isLoading = true
                page = 1
                val response = interactor.getAllUserCourses(page, currentFilter.value)
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _uiState.update { it.copy(canLoadMore = true) }
                    page++
                } else {
                    _uiState.update { it.copy(canLoadMore = false) }
                    page = -1
                }
                coursesList.clear()
                coursesList.addAll(response.courses)
                _uiState.update { it.copy(courses = coursesList.toList()) }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_no_connection)
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_unknown_error)
                        )
                    )
                }
            }
            _uiState.update { it.copy(refreshing = false, showProgress = false) }
            isLoading = false
        }
    }

    private fun internalLoadingCourses(courseStatusFilter: CourseStatusFilter? = null) {
        if (courseStatusFilter != null) {
            page = 1
            currentFilter.value = courseStatusFilter
        }
        job?.cancel()
        job = viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getAllUserCourses(page, currentFilter.value)
                } else {
                    null
                }
                if (response != null) {
                    if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                        _uiState.update { it.copy(canLoadMore = true) }
                        page++
                    } else {
                        _uiState.update { it.copy(canLoadMore = false) }
                        page = -1
                    }
                    coursesList.addAll(response.courses)
                } else {
                    val cachedList = interactor.getEnrolledCoursesFromCache()
                    _uiState.update { it.copy(canLoadMore = false) }
                    page = -1
                    coursesList.addAll(cachedList)
                }
                _uiState.update { it.copy(courses = coursesList.toList()) }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_no_connection)
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_unknown_error)
                        )
                    )
                }
            }
            _uiState.update { it.copy(refreshing = false, showProgress = false) }
            isLoading = false
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            internalLoadingCourses()
        }
    }

    private fun dashboardCourseClickedEvent(courseId: String, courseName: String) {
        analytics.dashboardCourseClickedEvent(courseId, courseName)
    }

    private fun collectDiscoveryNotifier() {
        viewModelScope.launch {
            discoveryNotifier.notifier.collect {
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }
    }

    fun navigateToCourseSearch(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToCourseSearch(
            fragmentManager,
            ""
        )
    }

    fun navigateToCourseOutline(
        fragmentManager: FragmentManager,
        courseId: String,
        courseName: String,
    ) {
        dashboardCourseClickedEvent(courseId, courseName)
        dashboardRouter.navigateToCourseOutline(
            fm = fragmentManager,
            courseId = courseId,
            courseTitle = courseName
        )
    }
}
