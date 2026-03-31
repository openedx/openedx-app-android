package org.openedx.dates.presentation

import androidx.fragment.app.FragmentManager

interface DatesRouter {

    fun navigateToSettings(fm: FragmentManager)

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        openTab: String,
        resumeBlockId: String
    )
}
