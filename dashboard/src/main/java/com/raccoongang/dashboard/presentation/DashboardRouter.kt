package com.raccoongang.dashboard.presentation

import androidx.fragment.app.FragmentManager

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String
    )

}