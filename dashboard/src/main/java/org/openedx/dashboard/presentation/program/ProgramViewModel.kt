package org.openedx.dashboard.presentation.program

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.dashboard.notifier.DashboardEvent
import org.openedx.dashboard.notifier.DashboardNotifier
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.core.R as coreR

class ProgramViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DashboardRouter,
    private val notifier: DashboardNotifier,
    private val edxCookieManager: AppCookieManager,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {
    val uriScheme: String get() = config.getUriScheme()

    val programConfig get() = config.getProgramConfig().webViewConfig

    var navigateToCourseDashboard: SingleEventLiveData<String> = SingleEventLiveData()
    var showEnrollmentError: SingleEventLiveData<String> = SingleEventLiveData()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val _uiState = MutableStateFlow<ProgramUIState>(ProgramUIState.Loading)
    val uiState: StateFlow<ProgramUIState>
        get() = _uiState.asStateFlow()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notifier.notifier.collectLatest {
                    if (it is DashboardEvent.CourseEnrolledSuccess && it.courseId.isNotEmpty()) {
                        if (it.courseId.isNotEmpty()) {
                            navigateToCourseDashboard.value = it.courseId
                            notifier.send(DashboardEvent.Empty)
                        }
                    } else if (it is DashboardEvent.CourseEnrolledError) {
                        if (it.exception.isInternetError()) {
                            _uiState.value = ProgramUIState.UiMessage(
                                UIMessage.SnackBarMessage(resourceManager.getString(coreR.string.core_error_no_connection))
                            )
                        } else {
                            showEnrollmentError.value = it.exception.message
                        }
                        notifier.send(DashboardEvent.Empty)
                    }
                }
            }
        }
    }

    fun enrollInACourse(courseId: String) {
        viewModelScope.launch {
            notifier.send(DashboardEvent.CourseEnrolled(courseId = courseId))
        }
    }

    fun onProgramCardClick(fragmentManager: FragmentManager, pathId: String) {
        if (pathId.isNotEmpty()) {
            router.navigateToProgramInfo(
                fm = fragmentManager,
                pathId = pathId
            )
        }
    }

    fun onViewCourseClick(fragmentManager: FragmentManager, courseId: String, infoType: String) {
        if (courseId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fm = fragmentManager,
                courseId = courseId,
                infoType = infoType
            )
        }
    }

    fun onEnrolledCourseClick(fragmentManager: FragmentManager, courseId: String) {
        if (courseId.isNotEmpty()) {
            router.navigateToCourseOutline(
                fm = fragmentManager,
                courseId = courseId,
                courseTitle = ""
            )
        }
    }

    fun navigateToDiscovery() {
        viewModelScope.launch {
            notifier.send(DashboardEvent.NavigationToDiscovery)
        }
    }

    fun refreshCookie() {
        viewModelScope.launch {
            edxCookieManager.tryToRefreshSessionCookie()
        }
    }
}
