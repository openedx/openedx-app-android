package org.openedx.auth.presentation.logistration

import androidx.fragment.app.FragmentManager
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.LogistrationAnalyticEvent
import org.openedx.auth.presentation.LogistrationAnalyticKey
import org.openedx.auth.presentation.LogistrationAnalyticValues
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
        logEvent(
            LogistrationAnalyticEvent.SIGN_IN_CLICKED,
            LogistrationAnalyticValues.SIGN_IN_CLICKED
        )
    }

    fun navigateToSignUp(parentFragmentManager: FragmentManager) {
        router.navigateToSignUp(parentFragmentManager, courseId, null)
        logEvent(
            LogistrationAnalyticEvent.REGISTER_CLICKED,
            LogistrationAnalyticValues.REGISTER_CLICKED
        )
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
                LogistrationAnalyticEvent.DISCOVERY_COURSES_SEARCH,
                LogistrationAnalyticValues.DISCOVERY_COURSES_SEARCH,
                buildMap { put(LogistrationAnalyticKey.LABEL.key, querySearch) })
        } ?: logEvent(
            LogistrationAnalyticEvent.EXPLORE_ALL_COURSES,
            LogistrationAnalyticValues.EXPLORE_ALL_COURSES
        )
    }

    private fun logEvent(
        eventName: LogistrationAnalyticEvent,
        biValue: LogistrationAnalyticValues,
        params: Map<String, Any?> = emptyMap()
    ) {
        analytics.logEvent(eventName.event, buildMap {
            put(LogistrationAnalyticKey.NAME.key, biValue.biValue)
            putAll(params)
        })
    }
}
