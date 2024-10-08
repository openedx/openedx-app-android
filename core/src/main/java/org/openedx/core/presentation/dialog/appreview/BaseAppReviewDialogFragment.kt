package org.openedx.core.presentation.dialog.appreview

import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.presentation.global.AppData
import org.openedx.foundation.extension.nonZero

open class BaseAppReviewDialogFragment : DialogFragment() {

    private val reviewPreferences: InAppReviewPreferences by inject()
    protected val appData: AppData by inject()
    protected val analytics: AppReviewAnalytics by inject()

    fun saveVersionName() {
        val versionName = appData.versionName
        reviewPreferences.setVersion(versionName)
    }

    fun onPositiveRate() {
        reviewPreferences.wasPositiveRated = true
    }

    fun onRatingDialogShowed() {
        analytics.logEvent(
            event = AppReviewAnalyticsEvent.RATING_DIALOG.eventName,
            params = buildMap {
                put(AppReviewAnalyticsKey.NAME.key, AppReviewAnalyticsEvent.RATING_DIALOG.biValue)
                put(AppReviewAnalyticsKey.CATEGORY.key, AppReviewAnalyticsKey.APP_REVIEWS.key)
            }
        )
    }

    fun notNowClick(rating: Int = 0) {
        saveVersionName()
        logDialogActionEvent(AppReviewAnalyticsKey.NOT_NOW.key, rating)
        super.dismiss()
    }

    fun onSubmitRatingClick(rating: Int) {
        logDialogActionEvent(AppReviewAnalyticsKey.SUBMIT.key, rating)
        super.dismiss()
    }

    fun onShareFeedbackClick() {
        logDialogActionEvent(AppReviewAnalyticsKey.SHARE_FEEDBACK.key)
        super.dismiss()
    }

    fun onRateAppClick() {
        logDialogActionEvent(AppReviewAnalyticsKey.RATE_APP.key)
        super.dismiss()
    }

    fun onDismiss() {
        logDialogActionEvent(AppReviewAnalyticsKey.DISMISSED.key)
        super.dismiss()
    }

    private fun logDialogActionEvent(action: String, rating: Int = 0) {
        analytics.logEvent(
            event = AppReviewAnalyticsEvent.RATING_DIALOG_ACTION.eventName,
            params = buildMap {
                put(
                    AppReviewAnalyticsKey.NAME.key,
                    AppReviewAnalyticsEvent.RATING_DIALOG_ACTION.biValue
                )
                put(AppReviewAnalyticsKey.CATEGORY.key, AppReviewAnalyticsKey.APP_REVIEWS.key)
                put(AppReviewAnalyticsKey.ACTION.key, action)
                rating.nonZero()?.let { put(AppReviewAnalyticsKey.RATING.key, it) }
            }
        )
    }
}
