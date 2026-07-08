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

    /** LMS Directory: open the "Find my LMS" browse/search screen. */
    fun navigateToLmsSelection(fm: FragmentManager)

    fun navigateToSignUp(fm: FragmentManager, courseId: String?, infoType: String?)

    fun navigateToRestorePassword(fm: FragmentManager)

    fun navigateToWhatsNew(fm: FragmentManager, courseId: String? = null, infoType: String? = null)

    fun navigateToWebDiscoverCourses(fm: FragmentManager, querySearch: String)

    fun navigateToNativeDiscoverCourses(fm: FragmentManager, querySearch: String)

    fun navigateToWebContent(fm: FragmentManager, title: String, url: String)

    fun navigateToSSOWebContent(fm: FragmentManager, title: String, url: String)

    fun clearBackStack(fm: FragmentManager)
}
