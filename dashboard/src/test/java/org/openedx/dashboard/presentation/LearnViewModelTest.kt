package org.openedx.dashboard.presentation

import androidx.fragment.app.FragmentManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openedx.DashboardNavigator
import org.openedx.core.config.Config
import org.openedx.core.config.DashboardConfig
import org.openedx.learn.presentation.LearnTab
import org.openedx.learn.presentation.LearnViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LearnViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val dashboardRouter = mockk<DashboardRouter>(relaxed = true)
    private val analytics = mockk<DashboardAnalytics>(relaxed = true)
    private val fragmentManager = mockk<FragmentManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSettingsClick calls navigateToSettings`() = runTest {
        val viewModel = LearnViewModel(LearnTab.COURSES.name, config, dashboardRouter, analytics)
        viewModel.onSettingsClick(fragmentManager)
        verify { dashboardRouter.navigateToSettings(fragmentManager) }
    }

    @Test
    fun `getDashboardFragment returns correct fragment based on dashboardType`() = runTest {
        val viewModel = LearnViewModel(LearnTab.COURSES.name, config, dashboardRouter, analytics)
        DashboardConfig.DashboardType.entries.forEach { type ->
            every { config.getDashboardConfig().getType() } returns type
            val dashboardFragment = viewModel.getDashboardFragment
            assertEquals(DashboardNavigator(type).getDashboardFragment()::class, dashboardFragment::class)
        }
    }

    @Test
    fun `getProgramFragment returns correct program fragment`() = runTest {
        val viewModel = LearnViewModel(LearnTab.COURSES.name, config, dashboardRouter, analytics)
        viewModel.getProgramFragment
        verify { dashboardRouter.getProgramFragment() }
    }

    @Test
    fun `isProgramTypeWebView returns correct view type`() = runTest {
        val viewModel = LearnViewModel(LearnTab.COURSES.name, config, dashboardRouter, analytics)
        every { config.getProgramConfig().isViewTypeWebView() } returns true
        assertTrue(viewModel.isProgramTypeWebView)
    }

    @Test
    fun `logMyCoursesTabClickedEvent logs correct analytics event`() = runTest {
        val viewModel = LearnViewModel(LearnTab.COURSES.name, config, dashboardRouter, analytics)
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
        val viewModel = LearnViewModel(LearnTab.COURSES.name, config, dashboardRouter, analytics)
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
