package org.openedx.dashboard.presentation.program

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.interfaces.EnrollInCourseInteractor
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.dashboard.notifier.DashboardEvent
import org.openedx.dashboard.notifier.DashboardNotifier
import org.openedx.dashboard.presentation.DashboardRouter

class ProgramViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DashboardRouter,
    private val notifier: DashboardNotifier,
    private val edxCookieManager: AppCookieManager,
    private val resourceManager: ResourceManager,
    private val courseInteractor: EnrollInCourseInteractor
) : BaseViewModel() {
    val uriScheme: String get() = config.getUriScheme()

    val programConfig get() = config.getProgramConfig().webViewConfig

    val hasInternetConnection: Boolean get() = networkConnection.isOnline()

    private val _uiState = MutableSharedFlow<ProgramUIState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiState: SharedFlow<ProgramUIState> get() = _uiState.asSharedFlow()

    fun showLoading(isLoading: Boolean) {
        viewModelScope.launch {
            _uiState.emit(if (isLoading) ProgramUIState.Loading else ProgramUIState.Loaded)
        }
    }

    fun enrollInACourse(courseId: String) {
        showLoading(true)
        viewModelScope.launch {
            try {
                courseInteractor.enrollInACourse(courseId)
                _uiState.emit(ProgramUIState.CourseEnrolled(courseId, true))
                notifier.send(DashboardEvent.UpdateEnrolledCourses)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiState.emit(
                        ProgramUIState.UiMessage(
                            UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                        )
                    )
                } else {
                    _uiState.emit(ProgramUIState.CourseEnrolled(courseId, false))
                }
            }
        }
    }

    fun onProgramCardClick(fragmentManager: FragmentManager, pathId: String) {
        if (pathId.isNotEmpty()) {
            router.navigateToProgramInfo(fm = fragmentManager, pathId = pathId)
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
                courseTitle = "",
                enrollmentMode = ""
            )
        }
    }

    fun navigateToDiscovery() {
        viewModelScope.launch { notifier.send(DashboardEvent.NavigationToDiscovery) }
    }

    fun refreshCookie() {
        viewModelScope.launch { edxCookieManager.tryToRefreshSessionCookie() }
    }
}
