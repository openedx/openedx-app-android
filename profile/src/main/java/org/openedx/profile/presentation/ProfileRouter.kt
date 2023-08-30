package org.openedx.profile.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.profile.domain.model.Account

interface ProfileRouter {

    fun navigateToEditProfile(fm: FragmentManager, account: Account)

    fun navigateToVideoSettings(fm: FragmentManager)

    fun navigateToVideoQuality(fm: FragmentManager)

    fun navigateToDeleteAccount(fm: FragmentManager)

    fun restartApp(fm: FragmentManager)

}