package com.raccoongang.profile.presentation.settings.video

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.VideoSettings
import com.raccoongang.profile.system.notifier.ProfileNotifier
import com.raccoongang.profile.system.notifier.VideoQualityChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VideoSettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val notifier: ProfileNotifier
) : BaseViewModel() {

    private val _videoSettings = MutableLiveData<VideoSettings>()
    val videoSettings: LiveData<VideoSettings>
        get() = _videoSettings

    val currentSettings: VideoSettings
        get() = preferencesManager.videoSettings

    init {
        _videoSettings.value = preferencesManager.videoSettings
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collectLatest {
                if (it is VideoQualityChanged) {
                    _videoSettings.value = preferencesManager.videoSettings
                }
            }
        }
    }

    fun setWifiDownloadOnly(value: Boolean) {
        val currentSettings = preferencesManager.videoSettings
        preferencesManager.videoSettings = currentSettings.copy(wifiDownloadOnly = value)
        _videoSettings.value = preferencesManager.videoSettings
    }

}