package org.openedx.learn.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.DashboardNavigator
import org.openedx.core.config.Config
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardAnalyticsEvent
import org.openedx.dashboard.presentation.DashboardAnalyticsKey
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.foundation.presentation.BaseViewModel

class LearnViewModel(
    private val config: Config,
    private val dashboardRouter: DashboardRouter,
    private val analytics: DashboardAnalytics,
) : BaseViewModel() {

    private val dashboardType get() = config.getDashboardConfig().getType()
    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    fun onSettingsClick(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToSettings(fragmentManager)
    }

    val getDashboardFragment get() = DashboardNavigator(dashboardType).getDashboardFragment()

    val getProgramFragment get() = dashboardRouter.getProgramFragment()

    fun logMyCoursesTabClickedEvent() {
        logScreenEvent(DashboardAnalyticsEvent.MY_COURSES)
    }

    fun logMyProgramsTabClickedEvent() {
        logScreenEvent(DashboardAnalyticsEvent.MY_PROGRAMS)
    }

    private fun logScreenEvent(event: DashboardAnalyticsEvent) {
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(DashboardAnalyticsKey.NAME.key, event.biValue)
            }
        )
    }
}
