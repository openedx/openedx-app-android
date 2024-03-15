package org.openedx.core.presentation.dialog.appreview

interface AppReviewAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class AppReviewEvent(val event: String) {
    RATING_DIALOG("AppReviews:Rating Dialog Viewed"),
    RATING_DIALOG_ACTION("AppReviews:Rating Dialog Action"),
}

enum class AppReviewValue(val biValue: String) {
    RATING_DIALOG("edx.bi.app.app_reviews.rating_dialog.viewed"),
    RATING_DIALOG_ACTION("edx.bi.app.app_reviews.rating_dialog.action"),
}

enum class AppReviewKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    RATING("rating"),
    APP_REVIEW("app_review"),
    ACTION("action"),
    DISMISSED("dismissed"),
    NOT_NOW("not_now"),
    SUBMIT("submit"),
    SHARE_FEEDBACK("share_feedback"),
    RATE_APP("rate_app"),
}
