package org.openedx.auth.presentation

import androidx.fragment.app.FragmentManager

interface AuthRouter {

    fun navigateToMain(fm: FragmentManager, courseId: String?)

    fun navigateToSignIn(fm: FragmentManager, courseId: String?)

    fun navigateToLogistration(fm: FragmentManager, courseId: String?)

    fun navigateToSignUp(fm: FragmentManager, courseId: String?)

    fun navigateToRestorePassword(fm: FragmentManager)

    fun navigateToWhatsNew(fm: FragmentManager)

    fun navigateToDiscoverCourses(fm: FragmentManager, querySearch: String)

    fun clearBackStack(fm: FragmentManager)
}
