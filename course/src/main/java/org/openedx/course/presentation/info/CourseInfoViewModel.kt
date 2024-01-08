package org.openedx.course.presentation.info

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseRouter
import org.openedx.core.R as CoreR

class CourseInfoViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: CourseRouter,
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _showAlert = MutableSharedFlow<Boolean>()
    val showAlert: SharedFlow<Boolean>
        get() = _showAlert.asSharedFlow()

    private val _courseEnrollSuccess = MutableSharedFlow<String>()
    val courseEnrollSuccess: SharedFlow<String>
        get() = _courseEnrollSuccess.asSharedFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    val uriScheme: String get() = config.getUriScheme()

    val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    fun enrollInACourse(courseId: String) {
        viewModelScope.launch {
            _showAlert.emit(false)
            try {
                val isCourseEnrolled = interactor.getEnrolledCourseFromCacheById(courseId) != null

                if (isCourseEnrolled) {
                    _uiMessage.emit(
                        UIMessage.ToastMessage(resourceManager.getString(R.string.course_you_are_already_enrolled))
                    )
                    _courseEnrollSuccess.emit(courseId)
                    return@launch
                }

                interactor.enrollInACourse(courseId)
                notifier.send(CourseDashboardUpdate())
                _courseEnrollSuccess.emit(courseId)

            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_error_no_connection))
                    )
                } else {
                    _showAlert.emit(true)
                }
            }
        }
    }

    fun onSuccessfulCourseEnrollment(fragmentManager: FragmentManager, courseId: String) {
        if (courseId.isNotEmpty()) {
            router.navigateToCourseOutline(
                fm = fragmentManager,
                courseId = courseId
            )
        }
    }

    fun infoCardClicked(fragmentManager: FragmentManager, pathId: String, infoType: String) {
        if (pathId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fm = fragmentManager,
                courseId = pathId,
                infoType = infoType
            )
        }
    }
}
