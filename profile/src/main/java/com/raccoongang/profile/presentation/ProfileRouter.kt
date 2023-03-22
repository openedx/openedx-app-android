package com.raccoongang.profile.presentation

import androidx.fragment.app.FragmentManager
import com.raccoongang.core.domain.model.Account

interface ProfileRouter {

    fun navigateToEditProfile(fm: FragmentManager, account: Account)

    fun navigateToVideoSettings(fm: FragmentManager)

    fun navigateToVideoQuality(fm: FragmentManager)

    fun navigateToDeleteAccount(fm: FragmentManager)

    fun restartApp(fm: FragmentManager)

}