package org.openedx.app

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.dashboard.notifier.DashboardEvent
import org.openedx.dashboard.notifier.DashboardNotifier

class MainViewModel(
    private val config: Config,
    private val notifier: DashboardNotifier,
    private val analytics: AppAnalytics,
) : BaseViewModel() {

    private val _isBottomBarEnabled = MutableLiveData(true)
    val isBottomBarEnabled: LiveData<Boolean>
        get() = _isBottomBarEnabled

    private val _navigateToDiscovery = MutableSharedFlow<Boolean>()
    val navigateToDiscovery: SharedFlow<Boolean>
        get() = _navigateToDiscovery.asSharedFlow()

    val isDiscoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()

    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        notifier.notifier.onEach {
            if (it is DashboardEvent.NavigationToDiscovery) {
                _navigateToDiscovery.emit(true)
            }
        }.distinctUntilChanged().launchIn(viewModelScope)
    }

    fun enableBottomBar(enable: Boolean) {
        _isBottomBarEnabled.value = enable
    }

    fun logDiscoveryTabClickedEvent() {
        logEvent(AppAnalyticsEvent.DISCOVER)
    }

    fun logMyCoursesTabClickedEvent() {
        logEvent(AppAnalyticsEvent.MY_COURSES)
    }

    fun logMyProgramsTabClickedEvent() {
        logEvent(AppAnalyticsEvent.MY_PROGRAMS)
    }

    fun logProfileTabClickedEvent() {
        logEvent(AppAnalyticsEvent.PROFILE)
    }

    private fun logEvent(event: AppAnalyticsEvent) {
        analytics.logEvent(event.eventName,
            buildMap {
                put(AppAnalyticsKey.NAME.key, event.biValue)
            }
        )
    }
}
