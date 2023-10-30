package org.openedx.core.presentation.dialog.app_review

import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.presentation.global.AppDataHolder

open class BaseAppReviewDialogFragment : DialogFragment() {

    private val reviewPreferences: InAppReviewPreferences by inject()

    fun saveVersionName() {
        //TODO Don't get the version name from the activity
        val versionName = (requireActivity() as AppDataHolder).appData.versionName
        reviewPreferences.setVersion(versionName)
    }

    fun onPositiveRate() {
        reviewPreferences.wasPositiveRated = true
    }

    fun notNowClick() {
        saveVersionName()
        dismiss()
    }
}