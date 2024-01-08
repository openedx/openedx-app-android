package org.openedx.dashboard.presentation.program

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.dashboard.presentation.DashboardRouter

class ProgramViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DashboardRouter,
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    private val edxCookieManager: AppCookieManager,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _showAlert = MutableSharedFlow<Boolean>()
    val showAlert: SharedFlow<Boolean>
        get() = _showAlert.asSharedFlow()

    private val _showLoading = MutableSharedFlow<Boolean>()
    val showLoading: SharedFlow<Boolean>
        get() = _showLoading.asSharedFlow()

    private val _courseEnrollSuccess = MutableSharedFlow<String>()
    val courseEnrollSuccess: SharedFlow<String>
        get() = _courseEnrollSuccess.asSharedFlow()

    val uriScheme: String get() = config.getUriScheme()

    val programConfig get() = config.getProgramConfig()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    fun enrollInACourse(courseId: String) {
        viewModelScope.launch {
            _showLoading.emit(true)
            try {
                interactor.enrollInACourse(courseId)
                notifier.send(CourseDashboardUpdate())
                _courseEnrollSuccess.emit(courseId)
                _showLoading.emit(false)

            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                    )
                } else {
                    _showAlert.emit(true)
                }
                _showLoading.emit(false)
            }
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

    fun refreshCookie() {
        viewModelScope.launch {
            edxCookieManager.tryToRefreshSessionCookie()
        }
    }
}
