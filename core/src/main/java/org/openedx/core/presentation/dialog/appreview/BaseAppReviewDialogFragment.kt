package org.openedx.core.presentation.dialog.appreview

import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.extension.nonZero
import org.openedx.core.presentation.global.AppData

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
            event = AppReviewEvent.RATING_DIALOG.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.RATING_DIALOG.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
            }
        )
    }

    fun notNowClick(rating: Int = 0) {
        saveVersionName()
        logDialogActionEvent(AppReviewKey.NOT_NOW.key, rating)
        dismiss()
    }

    fun onSubmitRatingClick(rating: Int) {
        logDialogActionEvent(AppReviewKey.SUBMIT.key, rating)
        dismiss()
    }

    fun onShareFeedbackClick() {
        logDialogActionEvent(AppReviewKey.SHARE_FEEDBACK.key)
        dismiss()
    }

    fun onRateAppClick() {
        logDialogActionEvent(AppReviewKey.RATE_APP.key)
        dismiss()
    }

    fun onDismiss() {
        logDialogActionEvent(AppReviewKey.DISMISSED.key)
        dismiss()
    }

    private fun logDialogActionEvent(action: String, rating: Int = 0) {
        analytics.logEvent(
            event = AppReviewEvent.RATING_DIALOG_ACTION.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.RATING_DIALOG_ACTION.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.ACTION.key, action)
                rating.nonZero()?.let { put(AppReviewKey.RATING.key, it) }
            }
        )
    }
}
