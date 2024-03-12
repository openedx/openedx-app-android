package org.openedx.whatsnew.presentation

interface WhatsNewAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class WhatsNewAnalyticEvent(val eventName: String) {
    WHATS_NEW_VIEW("WhatsNew:View"),
    WHATS_NEW_COMPLETED("WhatsNew:Completed"),
}

enum class WhatsNewAnalyticValue(val value: String) {
    SCREEN_NAVIGATION("edx.bi.app.navigation.screen"),
    WHATS_NEW("whats_new"),
}

enum class WhatsNewAnalyticKey(val key: String) {
    NAME("name"),
    APP_VERSION("app_version"),
    CATEGORY("category"),
    TOTAL_SCREENS("total_screens"),
}
