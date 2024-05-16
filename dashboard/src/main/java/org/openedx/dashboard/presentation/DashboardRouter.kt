package org.openedx.dashboard.presentation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        enrollmentMode: String,
        openDates: Boolean = false,
        openBlock: String = ""
    )

    fun navigateToSettings(fm: FragmentManager)

    fun navigateToCourseSearch(fm: FragmentManager, querySearch: String)

    fun navigateToAllEnrolledCourses(fm: FragmentManager)

    fun getProgramFragmentInstance(): Fragment
}
