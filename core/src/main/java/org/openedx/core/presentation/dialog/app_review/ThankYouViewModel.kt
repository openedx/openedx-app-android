package org.openedx.core.presentation.dialog.app_review

import android.app.Activity
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.model.ReviewErrorCode
import org.openedx.core.BaseViewModel

class ThankYouViewModel(
    private val reviewManager: ReviewManager,
) : BaseViewModel() {

    fun openInAppReview(activity: Activity) {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                }
            } else {
                @ReviewErrorCode
                val reviewErrorCode = (task.exception as ReviewException).errorCode
            }
        }
    }
}