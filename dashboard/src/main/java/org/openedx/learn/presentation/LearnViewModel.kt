package org.openedx.learn.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.DashboardNavigator
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.dashboard.presentation.DashboardRouter

class LearnViewModel(
    private val config: Config,
    private val dashboardRouter: DashboardRouter,
) : BaseViewModel() {

    private val dashboardType get() = config.getDashboardConfig().getType()
    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    fun onSettingsClick(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToSettings(fragmentManager)
    }

    val getDashboardFragment get() = DashboardNavigator(dashboardType).getDashboardFragment()

    val getProgramFragment get() = dashboardRouter.getProgramFragment()
}
