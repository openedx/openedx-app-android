package org.openedx.discovery.presentation.info

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.BaseViewModel
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.CoreAnalyticsKey
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.discovery.R
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryAnalyticsEvent
import org.openedx.discovery.presentation.DiscoveryAnalyticsKey
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.catalog.WebViewLink
import org.openedx.foundation.extension.isInternetError
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseInfoViewModel(
    val pathId: String,
    val infoType: String,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DiscoveryRouter,
    private val interactor: DiscoveryInteractor,
    private val notifier: DiscoveryNotifier,
    private val resourceManager: ResourceManager,
    private val analytics: DiscoveryAnalytics,
    corePreferences: CorePreferences,
) : BaseViewModel() {

    private val _uiState =
        MutableStateFlow(
            CourseInfoUIState(
                initialUrl = getInitialUrl(),
                isPreLogin = config.isPreLoginExperienceEnabled() && corePreferences.user == null
            )
        )
    internal val uiState: StateFlow<CourseInfoUIState> = _uiState

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _showAlert = MutableSharedFlow<Boolean>()
    val showAlert: SharedFlow<Boolean>
        get() = _showAlert.asSharedFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    val uriScheme: String get() = config.getUriScheme()

    private val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    private fun getInitialUrl(): String {
        val urlTemplate = when (infoType) {
            WebViewLink.Authority.COURSE_INFO.name -> webViewConfig.courseUrlTemplate
            WebViewLink.Authority.PROGRAM_INFO.name -> webViewConfig.programUrlTemplate
            else -> webViewConfig.baseUrl
        }
        return if (pathId.isEmpty() || infoType.isEmpty()) {
            webViewConfig.baseUrl
        } else {
            urlTemplate.replace("{${ARG_PATH_ID}}", pathId)
        }
    }

    fun enrollInACourse(courseId: String) {
        viewModelScope.launch {
            _showAlert.emit(false)
            try {
                val isCourseEnrolled = withContext(Dispatchers.IO) {
                    interactor.getCourseDetails(courseId)
                }.isEnrolled

                if (isCourseEnrolled) {
                    _uiMessage.emit(
                        UIMessage.ToastMessage(resourceManager.getString(R.string.discovery_you_are_already_enrolled))
                    )
                    _uiState.update { it.copy(enrollmentSuccess = AtomicReference(courseId)) }
                    return@launch
                }

                interactor.enrollInACourse(courseId)
                courseEnrollSuccessEvent(courseId)
                notifier.send(CourseDashboardUpdate())
                _uiMessage.emit(
                    UIMessage.ToastMessage(resourceManager.getString(R.string.discovery_enrolled_successfully))
                )
                _uiState.update { it.copy(enrollmentSuccess = AtomicReference(courseId)) }
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
                courseId = courseId,
                courseTitle = "",
                enrollmentMode = ""
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

    fun navigateToSignUp(fragmentManager: FragmentManager, courseId: String?, infoType: String) {
        router.navigateToSignUp(fragmentManager, courseId, infoType)
    }

    fun navigateToSignIn(fragmentManager: FragmentManager, courseId: String, infoType: String) {
        router.navigateToSignIn(fragmentManager, courseId, infoType)
    }

    fun courseInfoClickedEvent(courseId: String) {
        logScreenEvent(DiscoveryAnalyticsEvent.COURSE_INFO, courseId)
    }

    fun programInfoClickedEvent(courseId: String) {
        logScreenEvent(DiscoveryAnalyticsEvent.PROGRAM_INFO, courseId)
    }

    fun courseEnrollClickedEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_ENROLL_CLICKED, courseId)
    }

    private fun courseEnrollSuccessEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_ENROLL_SUCCESS, courseId)
    }

    private fun logEvent(
        event: DiscoveryAnalyticsEvent,
        courseId: String,
    ) {
        analytics.logEvent(event.eventName, buildEventDataMap(event, courseId))
    }

    private fun logScreenEvent(
        event: DiscoveryAnalyticsEvent,
        courseId: String,
    ) {
        analytics.logScreenEvent(event.eventName, buildEventDataMap(event, courseId))
    }

    private fun buildEventDataMap(
        event: DiscoveryAnalyticsEvent,
        courseId: String,
    ): Map<String, String> {
        return buildMap {
            put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
            put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
            put(DiscoveryAnalyticsKey.CATEGORY.key, CoreAnalyticsKey.DISCOVERY.key)
            put(DiscoveryAnalyticsKey.CONVERSION.key, courseId)
        }
    }

    companion object {
        private const val ARG_PATH_ID = "path_id"
    }
}
