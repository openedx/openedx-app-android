package org.openedx.whatsnew.presentation

interface WhatsNewAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class WhatsNewAnalyticsEvent(val eventName: String, val biValue: String) {
    WHATS_NEW_VIEW(
        "WhatsNew:Pop up Viewed",
        "edx.bi.app.whats_new.popup.viewed"
    ),
    WHATS_NEW_CLOSE(
        "WhatsNew:Close",
        "edx.bi.app.whats_new.close"
    ),
    WHATS_NEW_DONE(
        "WhatsNew:Done",
        "edx.bi.app.whats_new.done"
    ),
}

enum class WhatsNewAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    WHATS_NEW("whats_new"),
    TOTAL_SCREENS("total_screens"),
    CURRENTLY_VIEWED("currently_viewed"),
}
