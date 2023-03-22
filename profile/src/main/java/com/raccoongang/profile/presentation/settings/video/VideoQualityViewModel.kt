package com.raccoongang.profile.presentation.settings.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.VideoQuality
import com.raccoongang.profile.system.notifier.ProfileNotifier
import com.raccoongang.profile.system.notifier.VideoQualityChanged
import kotlinx.coroutines.launch

class VideoQualityViewModel(
    private val preferencesManager: PreferencesManager,
    private val notifier: ProfileNotifier
) : BaseViewModel() {

    private val _videoQuality = MutableLiveData<VideoQuality>()
    val videoQuality: LiveData<VideoQuality>
        get() = _videoQuality

    val currentVideoQuality = preferencesManager.videoSettings.videoQuality

    init {
        _videoQuality.value = preferencesManager.videoSettings.videoQuality
    }

    fun setVideoDownloadQuality(quality: VideoQuality) {
        val currentSettings = preferencesManager.videoSettings
        preferencesManager.videoSettings = currentSettings.copy(videoQuality = quality)
        _videoQuality.value = preferencesManager.videoSettings.videoQuality
        viewModelScope.launch {
            notifier.send(VideoQualityChanged())
        }
    }

}