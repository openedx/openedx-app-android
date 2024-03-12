package org.openedx.core.presentation.dialog.appreview

interface AppReviewAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class AppReviewEvent(val event: String) {
    RATING_DIALOG("AppReviews:Rating Dialog Viewed"),
    RATING_DIALOG_ACTION("AppReviews:Rating Dialog Action"),
    SHARE_FEEDBACK_DIALOG("AppReviews:Submit Feedback Dialog Viewed"),
    SHARE_FEEDBACK_DIALOG_ACTION("AppReviews:Submit Feedback Dialog Action"),
    THANKYOU_DIALOG("AppReviews:Thankyou Dialog Viewed"),
    THANKYOU_DIALOG_ACTION("AppReviews:Thankyou Dialog Action"),
}

enum class AppReviewValue(val biValue: String) {
    RATING_DIALOG("edx.bi.app.app_reviews.rating_alert.viewed"),
    RATING_DIALOG_ACTION("edx.bi.app.app_reviews.rating_alert.action"),
    SHARE_FEEDBACK_DIALOG("edx.bi.app.app_reviews.share_feedback.viewed"),
    SHARE_FEEDBACK_DIALOG_ACTION("edx.bi.app.app_reviews.share_feedback.action"),
    THANKYOU_DIALOG("edx.bi.app.app_reviews.thankyou_dialog.viewed"),
    THANKYOU_DIALOG_ACTION("edx.bi.app.app_reviews.thankyou_dialog.action"),
}

enum class AppReviewKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    RATING("rating"),
    APP_VERSION("app_version"),
    APP_REVIEW("app_review"),
    ACTION("action"),
    DISMISSED("dismissed"),
    NOT_NOW("not_now"),
    SUBMIT_FEEDBACK("submit_feedback"),
    SHARE_FEEDBACK("share_feedback"),
}
