package org.openedx.auth.presentation.logistration

import androidx.fragment.app.FragmentManager
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.LogistrationAnalyticKey
import org.openedx.auth.presentation.LogistrationAnalyticsEvent
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.extension.takeIfNotEmpty

class LogistrationViewModel(
    private val courseId: String,
    private val router: AuthRouter,
    private val config: Config,
    private val analytics: AuthAnalytics,
) : BaseViewModel() {

    private val discoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()

    fun navigateToSignIn(parentFragmentManager: FragmentManager) {
        router.navigateToSignIn(parentFragmentManager, courseId, null)
        logEvent(LogistrationAnalyticsEvent.SIGN_IN_CLICKED)
    }

    fun navigateToSignUp(parentFragmentManager: FragmentManager) {
        router.navigateToSignUp(parentFragmentManager, courseId, null)
        logEvent(LogistrationAnalyticsEvent.REGISTER_CLICKED)
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
                LogistrationAnalyticsEvent.DISCOVERY_COURSES_SEARCH,
                buildMap {
                    put(LogistrationAnalyticKey.SEARCH_QUERY.key, querySearch)
                })
        } ?: logEvent(LogistrationAnalyticsEvent.EXPLORE_ALL_COURSES)
    }

    private fun logEvent(
        event: LogistrationAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(LogistrationAnalyticKey.NAME.key, event.biValue)
                putAll(params)
            })
    }
}
