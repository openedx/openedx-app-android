package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager

interface DiscoveryRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
    )

    fun navigateToLogistration(fm: FragmentManager, courseId: String?)

    fun navigateToCourseDetail(fm: FragmentManager, courseId: String)

    fun navigateToCourseSearch(fm: FragmentManager, querySearch: String)

    fun navigateToUpgradeRequired(fm: FragmentManager)

    fun navigateToCourseInfo(fm: FragmentManager, courseId: String, infoType: String)

    fun navigateToSignUp(fm: FragmentManager, courseId: String? = null, infoType: String? = null)

    fun navigateToSignIn(fm: FragmentManager, courseId: String?, infoType: String?)

    fun navigateToSettings(fm: FragmentManager)

    fun navigateToEnrolledProgramInfo(fm: FragmentManager, pathId: String)
}
