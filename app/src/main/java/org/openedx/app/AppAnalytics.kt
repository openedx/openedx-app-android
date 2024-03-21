package org.openedx.app

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class AppAnalyticsEvent(val eventName: String, val biValue: String) {
    LAUNCH(
        "Launch",
        "edx.bi.app.launch"
    ),
    DISCOVER(
        "MainDashboard:Discover",
        "edx.bi.app.main_dashboard.discover"
    ),
    MY_COURSES(
        "MainDashboard:My Courses",
        "edx.bi.app.main_dashboard.my_course"
    ),
    MY_PROGRAMS(
        "MainDashboard:My Programs",
        "edx.bi.app.main_dashboard.my_program"
    ),
    PROFILE(
        "MainDashboard:Profile",
        "edx.bi.app.main_dashboard.profile"
    ),
}

enum class AppAnalyticKey(val key: String) {
    NAME("name"),
}
