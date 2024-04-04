package org.openedx.settings

import androidx.fragment.app.FragmentManager
import org.openedx.core.presentation.settings.VideoQualityType

interface SettingsRouter {
    fun restartApp(fm: FragmentManager, isLogistrationEnabled: Boolean)
    fun navigateToVideoSettings(fm: FragmentManager)
    fun navigateToVideoQuality(fm: FragmentManager, videoQualityType: VideoQualityType)
    fun navigateToWebContent(fm: FragmentManager, title: String, url: String)
}
