package org.openedx.dashboard.presentation.dashboard

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.AppUpgradeEvent
import org.openedx.core.system.notifier.AppUpgradeNotifier
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.notifier.DashboardEvent
import org.openedx.dashboard.notifier.DashboardNotifier


class DashboardViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val dashboardNotifier: DashboardNotifier,
    private val analytics: DashboardAnalytics,
    private val appUpgradeNotifier: AppUpgradeNotifier
) : BaseViewModel() {

    private val coursesList = mutableListOf<EnrolledCourse>()
    private var page = 1
    private var isLoading = false

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableLiveData<DashboardUIState>(DashboardUIState.Loading)
    val uiState: LiveData<DashboardUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            courseNotifier.notifier.collect {
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }
        viewModelScope.launch {
            dashboardNotifier.notifier.collect {
                if (it is DashboardEvent.CourseEnrolledSuccess) {
                    updateCourses()
                }
            }
        }
    }

    init {
        getCourses()
        collectAppUpgradeEvent()
    }

    fun getCourses() {
        _uiState.value = DashboardUIState.Loading
        coursesList.clear()
        internalLoadingCourses()
    }

    fun updateCourses() {
        viewModelScope.launch {
            try {
                _updating.value = true
                isLoading = true
                page = 1
                val response = interactor.getEnrolledCourses(page)
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
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(ArrayList(coursesList))
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
            _updating.value = false
            isLoading = false
        }
    }

    private fun internalLoadingCourses() {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getEnrolledCourses(page)
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
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(ArrayList(coursesList))
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
            _updating.value = false
            isLoading = false
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            internalLoadingCourses()
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appUpgradeNotifier.notifier.collect { event ->
                _appUpgradeEvent.value = event
            }
        }
    }

    fun dashboardCourseClickedEvent(courseId: String, courseName: String) {
        analytics.dashboardCourseClickedEvent(courseId, courseName)
    }

}