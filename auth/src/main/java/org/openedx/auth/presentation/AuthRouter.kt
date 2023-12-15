package org.openedx.auth.presentation

import androidx.fragment.app.FragmentManager

interface AuthRouter {

    fun navigateToMain(fm: FragmentManager, courseId: String?)

    fun navigateToSignIn(fm: FragmentManager, courseId: String? = null)

    fun navigateToLogistration(fm: FragmentManager, courseId: String? = null)

    fun navigateToSignUp(fm: FragmentManager, courseId: String? = null)

    fun navigateToRestorePassword(fm: FragmentManager)

    fun navigateToWhatsNew(fm: FragmentManager)

    fun navigateToDiscoverCourses(fm: FragmentManager, querySearch: String)

    fun clearBackStack(fm: FragmentManager)
}
