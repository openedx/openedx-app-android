package org.openedx.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.openedx.core.BaseViewModel

class MainViewModel: BaseViewModel() {
    private val _isBottomBarEnabled = MutableLiveData(true)
    val isBottomBarEnabled: LiveData<Boolean>
        get() = _isBottomBarEnabled

    fun enableBottomBar(enable: Boolean) {
        _isBottomBarEnabled.value = enable
    }
}