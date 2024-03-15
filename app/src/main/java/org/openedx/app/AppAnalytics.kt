package org.openedx.app

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
}


enum class AppAnalyticEvent(val event: String) {
    LAUNCH("Launch"),
    DISCOVER("MainDashboard:Discover"),
    MY_COURSES("MainDashboard:My Courses"),
    MY_PROGRAMS("MainDashboard:My Programs"),
    PROFILE("MainDashboard:Profile"),
}

enum class AppAnalyticValues(val value: String) {
    LAUNCH("edx.bi.app.launch"),
    DISCOVER("edx.bi.app.main_dashboard.discover"),
    MY_COURSES("edx.bi.app.main_dashboard.my_courses"),
    MY_PROGRAMS("edx.bi.app.main_dashboard.my_programs"),
    PROFILE("edx.bi.app.main_dashboard.profile"),
}

enum class AppAnalyticKey(val key: String) {
    NAME("name"),
}
