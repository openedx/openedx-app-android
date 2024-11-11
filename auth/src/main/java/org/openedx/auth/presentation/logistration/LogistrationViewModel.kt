package org.openedx.auth.presentation.logistration

import androidx.fragment.app.FragmentManager
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthAnalyticsEvent
import org.openedx.auth.presentation.AuthAnalyticsKey
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.config.Config
import org.openedx.foundation.extension.takeIfNotEmpty
import org.openedx.foundation.presentation.BaseViewModel

class LogistrationViewModel(
    private val courseId: String,
    private val router: AuthRouter,
    private val config: Config,
    private val analytics: AuthAnalytics,
) : BaseViewModel() {

    private val discoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()
    val isRegistrationEnabled get() = config.isRegistrationEnabled()

    init {
        logLogistrationScreenEvent()
    }

    fun navigateToSignIn(parentFragmentManager: FragmentManager) {
        router.navigateToSignIn(parentFragmentManager, courseId, null)
        logEvent(AuthAnalyticsEvent.SIGN_IN_CLICKED)
    }

    fun navigateToSignUp(parentFragmentManager: FragmentManager) {
        router.navigateToSignUp(parentFragmentManager, courseId, null)
        logEvent(AuthAnalyticsEvent.REGISTER_CLICKED)
    }

    fun navigateToDiscovery(parentFragmentManager: FragmentManager, querySearch: String) {
        if (discoveryTypeWebView) {
            router.navigateToWebDiscoverCourses(
                parentFragmentManager,
                querySearch
            )
        } else {
            router.navigateToNativeDiscoverCourses(
                parentFragmentManager,
                querySearch
            )
        }
        querySearch.takeIfNotEmpty()?.let {
            logEvent(
                event = AuthAnalyticsEvent.DISCOVERY_COURSES_SEARCH,
                params = buildMap {
                    put(AuthAnalyticsKey.SEARCH_QUERY.key, querySearch)
                }
            )
        } ?: logEvent(event = AuthAnalyticsEvent.EXPLORE_ALL_COURSES)
    }

    private fun logEvent(
        event: AuthAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(AuthAnalyticsKey.NAME.key, event.biValue)
                putAll(params)
            }
        )
    }

    private fun logLogistrationScreenEvent() {
        val event = AuthAnalyticsEvent.Logistration
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(AuthAnalyticsKey.NAME.key, event.biValue)
            }
        )
    }
}
