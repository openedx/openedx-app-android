package org.openedx.dashboard.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.presentation.course.CourseContainerTab

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        enrollmentMode: String,
        requiredTab: CourseContainerTab = CourseContainerTab.HOME,
        openBlock: String = ""
    )

    fun navigateToSettings(fm: FragmentManager)

    fun navigateToCourseSearch(fm: FragmentManager, querySearch: String)

    fun navigateToAllEnrolledCourses(fm: FragmentManager)
}
