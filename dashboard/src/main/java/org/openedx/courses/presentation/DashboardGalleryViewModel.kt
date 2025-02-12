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
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.model.CourseEnrollments
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil

class DashboardGalleryViewModel(
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val networkConnection: NetworkConnection,
    private val fileUtil: FileUtil,
    private val dashboardRouter: DashboardRouter,
    private val corePreferences: CorePreferences,
    private val windowSize: WindowSize,
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState =
        MutableStateFlow<DashboardGalleryUIState>(DashboardGalleryUIState.Loading)
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

    private var isLoading = false

    init {
        collectDiscoveryNotifier()
        getCourses()
    }

    fun getCourses() {
        viewModelScope.launch {
            try {
                val cachedCourseEnrollments = fileUtil.getObjectFromFile<CourseEnrollments>()
                if (cachedCourseEnrollments == null) {
                    if (networkConnection.isOnline()) {
                        _uiState.value = DashboardGalleryUIState.Loading
                    } else {
                        _uiState.value = DashboardGalleryUIState.Empty
                    }
                } else {
                    _uiState.value =
                        DashboardGalleryUIState.Courses(
                            cachedCourseEnrollments.mapToDomain(),
                            corePreferences.isRelativeDatesEnabled
                        )
                }
                if (networkConnection.isOnline()) {
                    isLoading = true
                    val pageSize = if (windowSize.isTablet) {
                        PAGE_SIZE_TABLET
                    } else {
                        PAGE_SIZE_PHONE
                    }
                    val response = interactor.getMainUserCourses(pageSize)
                    if (response.primary == null && response.enrollments.courses.isEmpty()) {
                        _uiState.value = DashboardGalleryUIState.Empty
                    } else {
                        _uiState.value = DashboardGalleryUIState.Courses(
                            response,
                            corePreferences.isRelativeDatesEnabled
                        )
                    }
                }
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
            } finally {
                _updating.value = false
                isLoading = false
            }
        }
    }

    fun updateCourses(isUpdating: Boolean = true) {
        if (isLoading) {
            return
        }
        _updating.value = isUpdating
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
        resumeBlockId: String = "",
    ) {
        dashboardRouter.navigateToCourseOutline(
            fm = fragmentManager,
            courseId = enrolledCourse.course.id,
            courseTitle = enrolledCourse.course.name,
            openTab = if (openDates) CourseTab.DATES.name else CourseTab.HOME.name,
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

    companion object {
        private const val PAGE_SIZE_TABLET = 7
        private const val PAGE_SIZE_PHONE = 5
    }
}
