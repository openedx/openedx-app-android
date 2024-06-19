package org.openedx.dashboard.presentation

interface DashboardAnalytics {
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
    fun dashboardCourseClickedEvent(courseId: String, courseName: String)
}

enum class DashboardAnalyticsEvent(val eventName: String, val biValue: String) {
    MY_COURSES(
        "MainDashboard:My Courses",
        "edx.bi.app.main_dashboard.my_course"
    ),
    MY_PROGRAMS(
        "MainDashboard:My Programs",
        "edx.bi.app.main_dashboard.my_program"
    ),
}

enum class DashboardAnalyticsKey(val key: String) {
    NAME("name"),
}
