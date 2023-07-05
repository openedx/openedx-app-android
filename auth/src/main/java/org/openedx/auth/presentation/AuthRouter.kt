package org.openedx.auth.presentation

import androidx.fragment.app.FragmentManager

interface AuthRouter {

    fun navigateToMain(fm: FragmentManager)

    fun navigateToSignUp(fm: FragmentManager)

    fun navigateToRestorePassword(fm: FragmentManager)

}