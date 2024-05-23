package org.openedx.courses.presentation

import androidx.fragment.app.FragmentManager
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
import org.openedx.core.data.model.CourseEnrollments
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.core.utils.FileUtil
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardRouter

class DashboardGalleryViewModel(
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val networkConnection: NetworkConnection,
    private val fileUtil: FileUtil,
    private val dashboardRouter: DashboardRouter
) : BaseViewModel() {

    companion object {
        private const val DATES_TAB = "dates"
    }

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableStateFlow<DashboardGalleryUIState>(DashboardGalleryUIState.Loading)
    val uiState: StateFlow<DashboardGalleryUIState>
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
                    if (response.primary == null && response.enrollments.courses.isEmpty()) {
                        _uiState.value = DashboardGalleryUIState.Empty
                    } else {
                        _uiState.value = DashboardGalleryUIState.Courses(response)
                    }
                } else {
                    val courseEnrollments = fileUtil.getObjectFromFile<CourseEnrollments>()
                    if (courseEnrollments == null) {
                        _uiState.value = DashboardGalleryUIState.Empty
                    } else {
                        _uiState.value = DashboardGalleryUIState.Courses(courseEnrollments.mapToDomain())
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

    fun navigateToAllEnrolledCourses(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToAllEnrolledCourses(fragmentManager)
    }

    fun navigateToCourseOutline(
        fragmentManager: FragmentManager,
        enrolledCourse: EnrolledCourse,
        openDates: Boolean = false,
        resumeBlockId: String = ""
    ) {
        dashboardRouter.navigateToCourseOutline(
            fm = fragmentManager,
            courseId = enrolledCourse.course.id,
            courseTitle = enrolledCourse.course.name,
            enrollmentMode = enrolledCourse.mode,
            openTab = if (openDates) DATES_TAB else "",
            resumeBlockId = resumeBlockId
        )
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

interface DashboardGalleryScreenAction {
    object SwipeRefresh : DashboardGalleryScreenAction
    object ViewAll : DashboardGalleryScreenAction
    object Reload : DashboardGalleryScreenAction
    object NavigateToDiscovery : DashboardGalleryScreenAction
    data class OpenBlock(val enrolledCourse: EnrolledCourse, val blockId: String) : DashboardGalleryScreenAction
    data class OpenCourse(val enrolledCourse: EnrolledCourse) : DashboardGalleryScreenAction
    data class NavigateToDates(val enrolledCourse: EnrolledCourse) : DashboardGalleryScreenAction
}
