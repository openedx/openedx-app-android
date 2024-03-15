package org.openedx.core.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.extension.Quadruple
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.CoreAnalyticsEvent
import org.openedx.core.presentation.CoreAnalyticsKey
import org.openedx.core.presentation.CoreAnalyticsValue
import org.openedx.core.system.notifier.VideoNotifier
import org.openedx.core.system.notifier.VideoQualityChanged

class VideoQualityViewModel(
    private val qualityType: String,
    private val preferencesManager: CorePreferences,
    private val notifier: VideoNotifier,
    private val analytics: CoreAnalytics,
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
        logVideoQualityChangedEvent(getCurrentVideoQuality(), quality)
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

    private fun logVideoQualityChangedEvent(oldQuality: VideoQuality, newQuality: VideoQuality) {
        val (event, biValue, oldValue, newValue) = if (getQualityType() == VideoQualityType.Streaming)
            Quadruple(
                CoreAnalyticsEvent.VIDEO_STREAMING_QUALITY_CHANGED.event,
                CoreAnalyticsValue.VIDEO_STREAMING_QUALITY_CHANGED.biValue,
                oldQuality.tagId,
                newQuality.tagId,
            )
        else
            Quadruple(
                CoreAnalyticsEvent.VIDEO_DOWNLOAD_QUALITY_CHANGED.event,
                CoreAnalyticsValue.VIDEO_DOWNLOAD_QUALITY_CHANGED.biValue,
                oldQuality.tagId,
                newQuality.tagId,
            )
        analytics.logEvent(
            event,
            mapOf(
                CoreAnalyticsKey.NAME.key to biValue,
                CoreAnalyticsKey.CATEGORY.key to CoreAnalyticsKey.PROFILE.key,
                CoreAnalyticsKey.VALUE.key to newValue,
                CoreAnalyticsKey.OLD_VALUE.key to oldValue,
            )
        )
    }
}
