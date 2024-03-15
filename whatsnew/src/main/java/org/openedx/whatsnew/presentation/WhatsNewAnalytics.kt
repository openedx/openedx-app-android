package org.openedx.whatsnew.presentation

interface WhatsNewAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class WhatsNewAnalyticEvent(val eventName: String) {
    WHATS_NEW_VIEW("WhatsNew:Pop up Viewed"),
    WHATS_NEW_DONE("WhatsNew:Done"),
}

enum class WhatsNewAnalyticValue(val value: String) {
    WHATS_NEW_VIEW("edx.bi.app.whats_new.popup.viewed"),
    WHATS_NEW_DONE("edx.bi.app.whats_new.done"),
}

enum class WhatsNewAnalyticKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    WHATS_NEW("whatsnew"),
    TOTAL_SCREENS("total_screens"),
}
