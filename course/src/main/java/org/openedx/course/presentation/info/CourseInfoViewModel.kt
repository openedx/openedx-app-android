package org.openedx.course.presentation.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor

class CourseInfoViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _showAlert = SingleEventLiveData<Boolean>()
    val showAlert: LiveData<Boolean>
        get() = _showAlert

    private val _courseEnrollSuccess = SingleEventLiveData<String>()
    val courseEnrollSuccess: LiveData<String>
        get() = _courseEnrollSuccess

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    fun enrollInACourse(courseId: String) {
        _showAlert.value = false
        viewModelScope.launch {
            try {
                val isCourseEnrolled = interactor.getEnrolledCourseFromCacheById(courseId) != null

                if (isCourseEnrolled) {
                    _uiMessage.value =
                        UIMessage.ToastMessage(resourceManager.getString(org.openedx.course.R.string.course_you_are_already_enrolled))
                    _courseEnrollSuccess.value = courseId
                    return@launch
                }

                interactor.enrollInACourse(courseId)
                notifier.send(CourseDashboardUpdate())
                _courseEnrollSuccess.value = courseId

            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _showAlert.value = true
                }
            }
        }
    }
}
