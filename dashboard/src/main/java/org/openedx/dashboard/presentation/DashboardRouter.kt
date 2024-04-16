package org.openedx.dashboard.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.CourseContainerTabEntity
import org.openedx.core.presentation.course.CourseViewMode

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        enrollmentMode: String,
        openTab: CourseContainerTabEntity
    )

    fun navigateToCourseContainer(
        fm: FragmentManager,
        courseId: String,
        unitId: String,
        componentId: String,
        mode: CourseViewMode
    )

    fun navigateToCourseSearch(fm: FragmentManager, querySearch: String)
}
