package org.openedx.dates.presentation

interface DatesAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class DatesAnalyticsEvent(val eventName: String, val biValue: String) {
    ASSIGNMENT_CLICK(
        "Dates:Assignment click",
        "edx.bi.app.dates.assignment_click"
    ),
    SHIFT_DUE_DATE_CLICK(
        "Dates:Shift due date click",
        "edx.bi.app.dates.shift_due_date_click"
    ),
}

enum class DatesAnalyticsKey(val key: String) {
    NAME("name"),
}
