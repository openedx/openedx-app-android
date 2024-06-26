package org.openedx.profile.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.presentation.settings.video.VideoQualityType
import org.openedx.profile.domain.model.Account

interface ProfileRouter {

    fun navigateToEditProfile(fm: FragmentManager, account: Account)

    fun navigateToDeleteAccount(fm: FragmentManager)

    fun navigateToSettings(fm: FragmentManager)

    fun restartApp(fm: FragmentManager, isLogistrationEnabled: Boolean)

    fun navigateToVideoSettings(fm: FragmentManager)

    fun navigateToVideoQuality(fm: FragmentManager, videoQualityType: VideoQualityType)

    fun navigateToWebContent(fm: FragmentManager, title: String, url: String)

    fun navigateToManageAccount(fm: FragmentManager)

    fun navigateToCoursesToSync(fm: FragmentManager)
}
