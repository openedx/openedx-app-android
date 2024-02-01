package org.openedx.profile.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.presentation.settings.VideoQualityType
import org.openedx.profile.domain.model.Account

interface ProfileRouter {

    fun navigateToEditProfile(fm: FragmentManager, account: Account)

    fun navigateToVideoSettings(fm: FragmentManager)

    fun navigateToVideoQuality(fm: FragmentManager, videoQualityType: VideoQualityType)

    fun navigateToDeleteAccount(fm: FragmentManager)

    fun navigateToWebContent(fm: FragmentManager, title: String, url: String)

    fun restartApp(fm: FragmentManager, isLogistrationEnabled: Boolean)
}
