package org.openedx.dashboard.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.CourseContainerTabEntity

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        enrollmentMode: String,
        openTab: CourseContainerTabEntity
    )

    fun navigateToCourseSearch(fm: FragmentManager, querySearch: String)

    fun navigateToAllEnrolledCourses(fm: FragmentManager)
}
