package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager

interface DiscoveryRouter {

    fun navigateToCourseDetail(fm: FragmentManager, courseId: String)

    fun navigateToCourseSearch(fm: FragmentManager)

    fun navigateToUpgradeRequired(fm: FragmentManager)

}