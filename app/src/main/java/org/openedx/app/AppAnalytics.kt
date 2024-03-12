package org.openedx.app

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
}


enum class AppAnalyticEvent(val event: String) {
    DISCOVER("MainDashboard:Discover"),
    MY_COURSES("MainDashboard:My Courses"),
    MY_PROGRAMS("MainDashboard:My Programs"),
    PROFILE("MainDashboard:Profile"),
}

enum class AppAnalyticValues(val value: String) {
    SCREEN_NAVIGATION("edx.bi.app.navigation.screen"),
}

enum class AppAnalyticKey(val key: String) {
    NAME("name"),
}
