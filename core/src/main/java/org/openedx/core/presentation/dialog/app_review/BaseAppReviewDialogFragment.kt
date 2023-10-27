package org.openedx.core.presentation.dialog.app_review

import androidx.fragment.app.DialogFragment

open class BaseAppReviewDialogFragment : DialogFragment() {

    fun notNowClick() {
        dismiss()
    }
}