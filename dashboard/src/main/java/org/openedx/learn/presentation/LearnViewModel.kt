package org.openedx.learn.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.dashboard.presentation.DashboardRouter

class LearnViewModel(
    private val config: Config,
    private val dashboardRouter: DashboardRouter
) : BaseViewModel() {

    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    fun onSearchClick(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToCourseSearch(fragmentManager, "")
    }
}
