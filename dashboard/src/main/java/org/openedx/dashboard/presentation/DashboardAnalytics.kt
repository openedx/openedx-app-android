package org.openedx.dashboard.presentation

interface DashboardAnalytics {
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
    fun dashboardCourseClickedEvent(courseId: String, courseName: String)
}

enum class DashboardAnalyticsEvent(val eventName: String, val biValue: String) {
    MY_COURSES(
        "Learn:My Courses",
        "edx.bi.app.main_dashboard.learn.my_course"
    ),
    MY_PROGRAMS(
        "Learn:My Programs",
        "edx.bi.app.main_dashboard.learn.my_programs"
    ),
}

enum class DashboardAnalyticsKey(val key: String) {
    NAME("name"),
}
