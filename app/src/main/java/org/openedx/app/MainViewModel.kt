package org.openedx.app

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.dashboard.notifier.DashboardNotifier
import org.openedx.dashboard.notifier.NavigationToDiscovery

class MainViewModel(
    private val config: Config,
    private val notifier: DashboardNotifier,
) : BaseViewModel() {

    private val _isBottomBarEnabled = MutableLiveData(true)
    val isBottomBarEnabled: LiveData<Boolean>
        get() = _isBottomBarEnabled

    private val _navigateToDiscovery = MutableSharedFlow<Boolean>()
    val navigateToDiscovery: SharedFlow<Boolean>
        get() = _navigateToDiscovery.asSharedFlow()

    val isDiscoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()

    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    init {
        runBlocking {
            _navigateToDiscovery.emit(false)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect{
                if(it is NavigationToDiscovery) {
                    _navigateToDiscovery.emit(true)
                }
            }
        }
    }

    fun enableBottomBar(enable: Boolean) {
        _isBottomBarEnabled.value = enable
    }
}
