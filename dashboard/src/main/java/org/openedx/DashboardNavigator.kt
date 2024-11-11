package org.openedx

import androidx.fragment.app.Fragment
import org.openedx.core.config.DashboardConfig
import org.openedx.courses.presentation.DashboardGalleryFragment
import org.openedx.dashboard.presentation.DashboardListFragment

class DashboardNavigator(
    private val dashboardType: DashboardConfig.DashboardType,
) {
    fun getDashboardFragment(): Fragment {
        return when (dashboardType) {
            DashboardConfig.DashboardType.GALLERY -> DashboardGalleryFragment()
            else -> DashboardListFragment()
        }
    }
}
