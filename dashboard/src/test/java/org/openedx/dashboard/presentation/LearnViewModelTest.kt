package org.openedx.dashboard.presentation

import androidx.fragment.app.FragmentManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.openedx.DashboardNavigator
import org.openedx.core.config.Config
import org.openedx.core.config.DashboardConfig
import org.openedx.learn.presentation.LearnViewModel

class LearnViewModelTest {

    private val config = mockk<Config>()
    private val dashboardRouter = mockk<DashboardRouter>(relaxed = true)
    private val analytics = mockk<DashboardAnalytics>(relaxed = true)
    private val fragmentManager = mockk<FragmentManager>()

    private val viewModel = LearnViewModel(config, dashboardRouter, analytics)

    @Test
    fun `onSettingsClick calls navigateToSettings`() = runTest {
        viewModel.onSettingsClick(fragmentManager)
        verify { dashboardRouter.navigateToSettings(fragmentManager) }
    }

    @Test
    fun `getDashboardFragment returns correct fragment based on dashboardType`() = runTest {
        DashboardConfig.DashboardType.entries.forEach { type ->
            every { config.getDashboardConfig().getType() } returns type
            val dashboardFragment = viewModel.getDashboardFragment
            assertEquals(DashboardNavigator(type).getDashboardFragment()::class, dashboardFragment::class)
        }
    }

    @Test
    fun `getProgramFragment returns correct program fragment`() = runTest {
        viewModel.getProgramFragment
        verify { dashboardRouter.getProgramFragment() }
    }

    @Test
    fun `isProgramTypeWebView returns correct view type`() = runTest {
        every { config.getProgramConfig().isViewTypeWebView() } returns true
        assertTrue(viewModel.isProgramTypeWebView)
    }

    @Test
    fun `logMyCoursesTabClickedEvent logs correct analytics event`() = runTest {
        viewModel.logMyCoursesTabClickedEvent()

        verify {
            analytics.logScreenEvent(
                screenName = DashboardAnalyticsEvent.MY_COURSES.eventName,
                params = match {
                    it[DashboardAnalyticsKey.NAME.key] == DashboardAnalyticsEvent.MY_COURSES.biValue
                }
            )
        }
    }

    @Test
    fun `logMyProgramsTabClickedEvent logs correct analytics event`() = runTest {
        viewModel.logMyProgramsTabClickedEvent()

        verify {
            analytics.logScreenEvent(
                screenName = DashboardAnalyticsEvent.MY_PROGRAMS.eventName,
                params = match {
                    it[DashboardAnalyticsKey.NAME.key] == DashboardAnalyticsEvent.MY_PROGRAMS.biValue
                }
            )
        }
    }
}
