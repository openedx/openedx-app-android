package org.openedx.auth.presentation

import androidx.fragment.app.FragmentManager

interface AuthRouter {

    fun navigateToMain(
        fm: FragmentManager,
        courseId: String?,
        infoType: String?,
        openTab: String = ""
    )

    fun navigateToSignIn(fm: FragmentManager, courseId: String?, infoType: String?)

    fun navigateToLogistration(fm: FragmentManager, courseId: String?)

    fun navigateToSignUp(fm: FragmentManager, courseId: String?, infoType: String?)

    fun navigateToRestorePassword(fm: FragmentManager)

    fun navigateToWhatsNew(fm: FragmentManager, courseId: String? = null, infoType: String? = null)

    fun navigateToWebDiscoverCourses(fm: FragmentManager, querySearch: String)

    fun navigateToNativeDiscoverCourses(fm: FragmentManager, querySearch: String)

    fun navigateToWebContent(fm: FragmentManager, title: String, url: String)

    fun clearBackStack(fm: FragmentManager)
}
