package org.openedx.dashboard.presentation.dashboard

interface DashboardAnalytics {
    fun dashboardCourseClickedEvent(courseId: String, courseName: String)
}
