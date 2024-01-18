package org.openedx.dashboard.presentation

import androidx.fragment.app.FragmentManager

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String
    )

    fun navigateToProgramInfo(
        fm: FragmentManager,
        pathId: String,

    )

    fun navigateToCourseInfo(fm: FragmentManager, courseId: String, infoType: String)
}
