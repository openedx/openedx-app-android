package org.openedx.core.presentation.dialog.appreview

import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.data.storage.InAppReviewPreferences
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
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
            }
        )
    }

    fun onFeedbackDialogShowed() {
        analytics.logEvent(
            event = AppReviewEvent.SHARE_FEEDBACK_DIALOG.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.SHARE_FEEDBACK_DIALOG.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
            }
        )
    }

    fun onThankYouDialogShowed() {
        analytics.logEvent(
            event = AppReviewEvent.THANKYOU_DIALOG.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.THANKYOU_DIALOG.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
            }
        )
    }

    fun notNowClick(dialogType: AppReviewDialogType, rating: Int = 0) {
        saveVersionName()
        analytics.logEvent(
            event = dialogType.event.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, dialogType.biValue.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
                put(AppReviewKey.ACTION.key, AppReviewKey.NOT_NOW.key)
                put(AppReviewKey.RATING.key, rating)
            }
        )
        dismiss()
    }

    fun onSubmitRatingClick(rating: Int) {
        analytics.logEvent(
            event = AppReviewEvent.RATING_DIALOG_ACTION.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.RATING_DIALOG_ACTION.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
                put(AppReviewKey.ACTION.key, AppReviewKey.SUBMIT_FEEDBACK.key)
                put(AppReviewKey.RATING.key, rating)
            }
        )
        dismiss()
    }

    fun onShareFeedbackClick() {
        analytics.logEvent(
            event = AppReviewEvent.SHARE_FEEDBACK_DIALOG_ACTION.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.SHARE_FEEDBACK_DIALOG_ACTION.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
                put(AppReviewKey.ACTION.key, AppReviewKey.SHARE_FEEDBACK.key)
            }
        )
        dismiss()
    }

    fun onSendFeedbackClick() {
        analytics.logEvent(
            event = AppReviewEvent.SHARE_FEEDBACK_DIALOG.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, AppReviewValue.SHARE_FEEDBACK_DIALOG.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
            }
        )
        dismiss()
    }

    fun onDismiss(dialogType: AppReviewDialogType) {
        analytics.logEvent(
            event = dialogType.event.event,
            params = buildMap {
                put(AppReviewKey.NAME.key, dialogType.biValue.biValue)
                put(AppReviewKey.CATEGORY.key, AppReviewKey.APP_REVIEW.key)
                put(AppReviewKey.APP_VERSION.key, appData.versionName)
                put(AppReviewKey.ACTION.key, AppReviewKey.DISMISSED.key)
            }
        )
        dismiss()
    }
}

enum class AppReviewDialogType(val event: AppReviewEvent, val biValue: AppReviewValue) {
    RATE(AppReviewEvent.RATING_DIALOG_ACTION, AppReviewValue.RATING_DIALOG_ACTION),
    FEEDBACK(
        AppReviewEvent.SHARE_FEEDBACK_DIALOG_ACTION,
        AppReviewValue.SHARE_FEEDBACK_DIALOG_ACTION
    ),
    THANK_YOU(AppReviewEvent.THANKYOU_DIALOG_ACTION, AppReviewValue.THANKYOU_DIALOG_ACTION),
}
