package org.openedx.learn.presentation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.DashboardNavigator
import org.openedx.core.config.Config
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardAnalyticsEvent
import org.openedx.dashboard.presentation.DashboardAnalyticsKey
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.learn.LearnType

class LearnViewModel(
    openTab: String,
    private val config: Config,
    private val dashboardRouter: DashboardRouter,
    private val analytics: DashboardAnalytics,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(
        LearnUIState(
            if (openTab == LearnTab.PROGRAMS.name) {
                LearnType.PROGRAMS
            } else {
                LearnType.COURSES
            }
        )
    )

    val uiState: StateFlow<LearnUIState>
        get() = _uiState.asStateFlow()

    private val dashboardType get() = config.getDashboardConfig().getType()
    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    fun onSettingsClick(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToSettings(fragmentManager)
    }

    val getDashboardFragment get() = DashboardNavigator(dashboardType).getDashboardFragment()

    val getProgramFragment get() = dashboardRouter.getProgramFragment()

    init {
        viewModelScope.launch {
            _uiState.collect { uiState ->
                if (uiState.learnType == LearnType.COURSES) {
                    logMyCoursesTabClickedEvent()
                } else {
                    logMyProgramsTabClickedEvent()
                }
            }
        }
    }

    fun updateLearnType(learnType: LearnType) {
        viewModelScope.launch {
            _uiState.update { it.copy(learnType = learnType) }
        }
    }

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
