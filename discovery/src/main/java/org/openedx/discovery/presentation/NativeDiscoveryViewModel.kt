package org.openedx.discovery.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.domain.model.Course
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class NativeDiscoveryViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DiscoveryInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: DiscoveryAnalytics,
    private val appNotifier: AppNotifier,
    private val corePreferences: CorePreferences,
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()
    val isUserLoggedIn get() = corePreferences.user != null
    val canShowBackButton get() = config.isPreLoginExperienceEnabled() && !isUserLoggedIn
    val isRegistrationEnabled: Boolean get() = config.isRegistrationEnabled()

    private val _uiState = MutableLiveData<DiscoveryUIState>(DiscoveryUIState.Loading)
    val uiState: LiveData<DiscoveryUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private var page = 1
    private val coursesList = mutableListOf<Course>()
    private var isLoading = false

    init {
        getCoursesList()
        collectAppUpgradeEvent()
    }

    private fun loadCoursesInternal(
        username: String? = null,
        organization: String? = null
    ) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getCoursesList(username, organization, page)
                } else {
                    null
                }
                if (response != null) {
                    if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                        _canLoadMore.value = true
                        page++
                    } else {
                        _canLoadMore.value = false
                        page = -1
                    }
                    coursesList.addAll(response.results)
                } else {
                    val cachedList = interactor.getCoursesListFromCache()
                    _canLoadMore.value = false
                    page = -1
                    coursesList.addAll(cachedList)
                }
                _uiState.value = DiscoveryUIState.Courses(ArrayList(coursesList))
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun getCoursesList(
        username: String? = null,
        organization: String? = null
    ) {
        _uiState.value = DiscoveryUIState.Loading
        coursesList.clear()
        loadCoursesInternal(username, organization)
    }

    fun updateData(
        username: String? = null,
        organization: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isUpdating.value = true
                isLoading = true
                page = 1
                val response = interactor.getCoursesList(username, organization, page)
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                coursesList.clear()
                coursesList.addAll(response.results)
                _uiState.value = DiscoveryUIState.Courses(ArrayList(coursesList))
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                isLoading = false
                _isUpdating.value = false
            }
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            loadCoursesInternal()
        }
    }

    @OptIn(FlowPreview::class)
    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appNotifier.notifier
                .debounce(100)
                .collect { event ->
                    when (event) {
                        is AppUpgradeEvent.UpgradeRecommendedEvent -> {
                            _appUpgradeEvent.value = event
                        }
                        is AppUpgradeEvent.UpgradeRequiredEvent -> {
                            _appUpgradeEvent.value = AppUpgradeEvent.UpgradeRequiredEvent
                        }
                    }
                }
        }
    }

    fun discoverySearchBarClickedEvent() {
        analytics.discoverySearchBarClickedEvent()
    }

    fun discoveryCourseClicked(courseId: String, courseName: String) {
        analytics.discoveryCourseClickedEvent(courseId, courseName)
    }

    fun courseDetailClickedEvent(courseId: String, courseTitle: String) {
        val event = DiscoveryAnalyticsEvent.COURSE_INFO
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
                put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
                put(DiscoveryAnalyticsKey.COURSE_NAME.key, courseTitle)
                put(DiscoveryAnalyticsKey.CATEGORY.key, DiscoveryAnalyticsKey.DISCOVERY.key)
            }
        )
    }
}
