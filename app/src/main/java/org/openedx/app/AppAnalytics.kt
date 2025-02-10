package org.openedx.app

interface AppAnalytics {
    fun logoutEvent(force: Boolean)
    fun setUserIdForSession(userId: Long)
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class AppAnalyticsEvent(val eventName: String, val biValue: String) {
    LAUNCH(
        "Launch",
        "edx.bi.app.launch"
    ),
    LEARN(
        "MainDashboard:Learn",
        "edx.bi.app.main_dashboard.learn"
    ),
    DISCOVER(
        "MainDashboard:Discover",
        "edx.bi.app.main_dashboard.discover"
    ),
    PROFILE(
        "MainDashboard:Profile",
        "edx.bi.app.main_dashboard.profile"
    ),
}

enum class AppAnalyticsKey(val key: String) {
    NAME("name"),
}
