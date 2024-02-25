package org.openedx.core.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.system.notifier.VideoNotifier
import org.openedx.core.system.notifier.VideoQualityChanged

class VideoQualityViewModel(
    private val preferencesManager: CorePreferences,
    private val notifier: VideoNotifier,
    private val qualityType: String
) : BaseViewModel() {

    private val _videoQuality = MutableLiveData<VideoQuality>()
    val videoQuality: LiveData<VideoQuality>
        get() = _videoQuality

    init {
        _videoQuality.value = getCurrentVideoQuality()
    }

    fun getCurrentVideoQuality(): VideoQuality {
        return if (getQualityType() == VideoQualityType.Streaming)
            preferencesManager.videoSettings.videoStreamingQuality else
            preferencesManager.videoSettings.videoDownloadQuality
    }

    fun setVideoQuality(quality: VideoQuality) {
        val currentSettings = preferencesManager.videoSettings
        if (getQualityType() == VideoQualityType.Streaming) {
            preferencesManager.videoSettings = currentSettings.copy(videoStreamingQuality = quality)
        } else {
            preferencesManager.videoSettings = currentSettings.copy(videoDownloadQuality = quality)
        }
        _videoQuality.value = getCurrentVideoQuality()
        viewModelScope.launch {
            notifier.send(VideoQualityChanged())
        }
    }

    fun getQualityType() = VideoQualityType.valueOf(qualityType)
}
