package org.openedx.courses.presentation

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
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.core.utils.FileUtil
import org.openedx.courses.domain.model.UserCourses
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardRouter

class PrimaryCourseViewModel(
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val networkConnection: NetworkConnection,
    private val fileUtil: FileUtil,
    val dashboardRouter: DashboardRouter
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableStateFlow<PrimaryCourseUIState>(PrimaryCourseUIState.Loading)
    val uiState: StateFlow<PrimaryCourseUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage?>
        get() = _uiMessage.asSharedFlow()

    private val _updating = MutableStateFlow<Boolean>(false)
    val updating: StateFlow<Boolean>
        get() = _updating.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        collectDiscoveryNotifier()
        getCourses()
    }

    fun getCourses() {
        viewModelScope.launch {
            try {
                if (networkConnection.isOnline()) {
                    val response = interactor.getMainUserCourses()
                    if (response.primary == null && response.enrollments.isEmpty()) {
                        _uiState.value = PrimaryCourseUIState.Empty
                    } else {
                        _uiState.value = PrimaryCourseUIState.Courses(response)
                    }
                } else {
                    val cachedUserCourses = fileUtil.getObjectFromFile<UserCourses>()
                    if (cachedUserCourses == null) {
                        _uiState.value = PrimaryCourseUIState.Empty
                    } else {
                        _uiState.value = PrimaryCourseUIState.Courses(cachedUserCourses)
                    }
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            } finally {
                _updating.value = false
            }
        }
    }

    fun updateCourses() {
        _updating.value = true
        getCourses()
    }

    fun navigateToDiscovery() {
        viewModelScope.launch { discoveryNotifier.send(NavigationToDiscovery()) }
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
}

interface PrimaryCourseScreenAction {
    object SwipeRefresh : PrimaryCourseScreenAction
    object ViewAll : PrimaryCourseScreenAction
    object Reload : PrimaryCourseScreenAction
    object NavigateToDiscovery : PrimaryCourseScreenAction
    data class OpenBlock(val enrolledCourse: EnrolledCourse, val blockId: String) : PrimaryCourseScreenAction
    data class OpenCourse(val enrolledCourse: EnrolledCourse) : PrimaryCourseScreenAction
    data class NavigateToDates(val enrolledCourse: EnrolledCourse) : PrimaryCourseScreenAction
}
