package org.openedx.discovery.presentation.program

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class ProgramViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DiscoveryRouter,
    private val notifier: DiscoveryNotifier,
    private val edxCookieManager: AppCookieManager,
    private val resourceManager: ResourceManager,
    private val interactor: DiscoveryInteractor,
) : BaseViewModel() {
    val uriScheme: String get() = config.getUriScheme()

    val programConfig get() = config.getProgramConfig().webViewConfig

    val cookieManager get() = edxCookieManager

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
                interactor.enrollInACourse(courseId)
                _uiState.emit(ProgramUIState.CourseEnrolled(courseId, true))
                notifier.send(CourseDashboardUpdate())
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
            router.navigateToEnrolledProgramInfo(fm = fragmentManager, pathId = pathId)
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
        viewModelScope.launch { notifier.send(NavigationToDiscovery()) }
    }

    fun navigateToSettings(fragmentManager: FragmentManager) {
        router.navigateToSettings(fragmentManager)
    }
}
