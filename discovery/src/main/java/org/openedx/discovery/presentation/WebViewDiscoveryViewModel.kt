package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.utils.UrlUtils

class WebViewDiscoveryViewModel(
    private val querySearch: String,
    private val appData: AppData,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val router: DiscoveryRouter,
    private val analytics: DiscoveryAnalytics,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<WebViewUIState>(WebViewUIState.Loading)
    val uiState: StateFlow<WebViewUIState> = _uiState.asStateFlow()
    val uriScheme: String get() = config.getUriScheme()

    private val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    val isPreLogin get() = config.isPreLoginExperienceEnabled() && corePreferences.user == null
    val isRegistrationEnabled: Boolean get() = config.isRegistrationEnabled()

    val appUserAgent get() = appData.appUserAgent

    private var _discoveryUrl = webViewConfig.baseUrl
    val discoveryUrl: String
        get() {
            return if (querySearch.isNotBlank()) {
                val queryParams: MutableMap<String, String> = HashMap()
                queryParams[UrlUtils.QUERY_PARAM_SEARCH] = querySearch
                UrlUtils.buildUrlWithQueryParams(_discoveryUrl, queryParams)
            } else {
                _discoveryUrl
            }
        }

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    fun onWebPageLoading() {
        _uiState.value = WebViewUIState.Loading
    }

    fun onWebPageLoaded() {
        _uiState.value = WebViewUIState.Loaded
    }

    fun onWebPageLoadError() {
        _uiState.value = WebViewUIState.Error(
            if (networkConnection.isOnline()) {
                ErrorType.UNKNOWN_ERROR
            } else {
                ErrorType.CONNECTION_ERROR
            }
        )
    }

    fun updateDiscoveryUrl(url: String) {
        if (url.isNotEmpty()) {
            _discoveryUrl = url
        }
    }

    fun infoCardClicked(fragmentManager: FragmentManager, pathId: String, infoType: String) {
        if (pathId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fragmentManager,
                pathId,
                infoType
            )
        }
    }

    fun navigateToSignUp(fragmentManager: FragmentManager) {
        router.navigateToSignUp(fragmentManager, null)
    }

    fun navigateToSignIn(fragmentManager: FragmentManager) {
        router.navigateToSignIn(fragmentManager, null, null)
    }

    fun navigateToSettings(fragmentManager: FragmentManager) {
        router.navigateToSettings(fragmentManager)
    }

    fun courseInfoClickedEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.COURSE_INFO, courseId)
    }

    fun programInfoClickedEvent(courseId: String) {
        logEvent(DiscoveryAnalyticsEvent.PROGRAM_INFO, courseId)
    }

    private fun logEvent(
        event: DiscoveryAnalyticsEvent,
        courseId: String,
    ) {
        analytics.logScreenEvent(
            event.eventName,
            buildMap {
                put(DiscoveryAnalyticsKey.NAME.key, event.biValue)
                put(DiscoveryAnalyticsKey.COURSE_ID.key, courseId)
                put(DiscoveryAnalyticsKey.CATEGORY.key, DiscoveryAnalyticsKey.DISCOVERY.key)
            }
        )
    }
}
