package org.openedx.dashboard.presentation

interface DashboardAnalytics {
    fun dashboardCourseClickedEvent(courseId: String, courseName: String)
}
