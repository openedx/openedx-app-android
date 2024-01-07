package org.openedx.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config

class MainViewModel(
    private val config: Config
) : BaseViewModel() {

    private val _isBottomBarEnabled = MutableLiveData(true)
    val isBottomBarEnabled: LiveData<Boolean>
        get() = _isBottomBarEnabled

    val isDiscoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()

    val isMyProgramsEnabled get() = config.getProgramConfig().enabled

    fun enableBottomBar(enable: Boolean) {
        _isBottomBarEnabled.value = enable
    }
}
