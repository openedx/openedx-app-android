package org.openedx.settings.presentation.video

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.presentation.settings.VideoQualityType
import org.openedx.core.system.notifier.VideoNotifier
import org.openedx.core.system.notifier.VideoQualityChanged
import org.openedx.settings.SettingsAnalytics
import org.openedx.settings.SettingsAnalyticsEvent
import org.openedx.settings.SettingsAnalyticsKey
import org.openedx.settings.SettingsRouter

class VideoSettingsViewModel(
    private val preferencesManager: CorePreferences,
    private val notifier: VideoNotifier,
    private val analytics: SettingsAnalytics,
    private val router: SettingsRouter,
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
        logProfileEvent(
           SettingsAnalyticsEvent.WIFI_TOGGLE,
            buildMap {
                put(
                    SettingsAnalyticsKey.ACTION.key,
                    if (value) SettingsAnalyticsKey.TRUE.key else SettingsAnalyticsKey.FALSE.key
                )
            }
        )
    }

    fun navigateToVideoStreamingQuality(fragmentManager: FragmentManager) {
        router.navigateToVideoQuality(
            fragmentManager, VideoQualityType.Streaming
        )
    }

    fun navigateToVideoDownloadQuality(fragmentManager: FragmentManager) {
        router.navigateToVideoQuality(
            fragmentManager, VideoQualityType.Download
        )
    }

    private fun logProfileEvent(
        event: SettingsAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(SettingsAnalyticsKey.NAME.key, event.biValue)
                put(SettingsAnalyticsKey.CATEGORY.key, SettingsAnalyticsKey.PROFILE.key)
                putAll(params)
            }
        )
    }
}
