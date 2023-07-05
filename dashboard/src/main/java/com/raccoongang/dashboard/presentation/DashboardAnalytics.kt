package com.raccoongang.dashboard.presentation

interface DashboardAnalytics {
    fun dashboardCourseClickedEvent(courseId: String, courseName: String)
}
