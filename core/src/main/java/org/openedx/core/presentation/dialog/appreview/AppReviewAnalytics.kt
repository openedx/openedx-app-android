package org.openedx.core.presentation.dialog.appreview

interface AppReviewAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class AppReviewAnalyticsEvent(val eventName: String, val biValue: String) {
    RATING_DIALOG(
        "AppReviews:Rating Dialog Viewed",
        "edx.bi.app.app_reviews.rating_dialog.viewed"
    ),
    RATING_DIALOG_ACTION(
        "AppReviews:Rating Dialog Action",
        "edx.bi.app.app_reviews.rating_dialog.action"
    ),
}

enum class AppReviewAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    RATING("rating"),
    APP_REVIEWS("app_reviews"),
    ACTION("action"),
    DISMISSED("dismissed"),
    NOT_NOW("not_now"),
    SUBMIT("submit"),
    SHARE_FEEDBACK("share_feedback"),
    RATE_APP("rate_app"),
}
