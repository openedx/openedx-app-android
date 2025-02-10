package org.openedx.discovery.presentation.program

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.ErrorType
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
    private val appData: AppData,
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

    val appUserAgent get() = appData.appUserAgent

    private val _uiState = MutableStateFlow<ProgramUIState>(ProgramUIState.Loading)
    val uiState: StateFlow<ProgramUIState> get() = _uiState.asStateFlow()

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
            )
        }
        viewModelScope.launch {
            _uiState.emit(ProgramUIState.Loaded)
        }
    }

    fun navigateToDiscovery() {
        viewModelScope.launch { notifier.send(NavigationToDiscovery()) }
    }

    fun navigateToSettings(fragmentManager: FragmentManager) {
        router.navigateToSettings(fragmentManager)
    }

    fun onPageLoadError() {
        viewModelScope.launch {
            _uiState.emit(
                ProgramUIState.Error(
                    if (networkConnection.isOnline()) {
                        ErrorType.UNKNOWN_ERROR
                    } else {
                        ErrorType.CONNECTION_ERROR
                    }
                )
            )
        }
    }
}
