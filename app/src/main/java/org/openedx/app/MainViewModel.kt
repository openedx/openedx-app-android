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
import kotlinx.coroutines.launch
import org.openedx.core.config.Config
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.discovery.presentation.DiscoveryNavigator
import org.openedx.foundation.presentation.BaseViewModel

class MainViewModel(
    private val config: Config,
    private val notifier: DiscoveryNotifier,
    private val analytics: AppAnalytics,
    private val appNotifier: AppNotifier,
) : BaseViewModel() {

    private val _isBottomBarEnabled = MutableLiveData(true)
    val isBottomBarEnabled: LiveData<Boolean>
        get() = _isBottomBarEnabled

    private val _navigateToDiscovery = MutableSharedFlow<Boolean>()
    val navigateToDiscovery: SharedFlow<Boolean>
        get() = _navigateToDiscovery.asSharedFlow()

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    val isDiscoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()
    val getDiscoveryFragment get() = DiscoveryNavigator(isDiscoveryTypeWebView).getDiscoveryFragment()

    val isDownloadsFragmentEnabled get() = config.getDownloadsConfig().isEnabled

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        collectDiscoveryEvents()
        collectAppUpgradeEvent()
    }

    fun enableBottomBar(enable: Boolean) {
        _isBottomBarEnabled.value = enable
    }

    fun logLearnTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.LEARN)
    }

    fun logDiscoveryTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.DISCOVER)
    }

    fun logDownloadsTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.DOWNLOADS)
    }

    fun logDatesTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.DATES)
    }

    fun logProfileTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.PROFILE)
    }

    private fun logScreenEvent(event: AppAnalyticsEvent) {
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(AppAnalyticsKey.NAME.key, event.biValue)
            }
        )
    }

    private fun collectDiscoveryEvents() {
        notifier.notifier
            .onEach {
                if (it is NavigationToDiscovery) {
                    _navigateToDiscovery.emit(true)
                }
            }
            .distinctUntilChanged()
            .launchIn(viewModelScope)
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appNotifier.notifier
                .onEach { event ->
                    if (event is AppUpgradeEvent) {
                        _appUpgradeEvent.value = event
                    }
                }
                .distinctUntilChanged()
                .launchIn(viewModelScope)
        }
    }
}
