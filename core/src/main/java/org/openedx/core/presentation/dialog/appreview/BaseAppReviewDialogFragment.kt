package org.openedx.core.presentation.dialog.appreview

import androidx.fragment.app.DialogFragment
import org.koin.android.ext.android.inject
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.presentation.global.AppData

open class BaseAppReviewDialogFragment : DialogFragment() {

    private val reviewPreferences: InAppReviewPreferences by inject()
    protected val appData: AppData by inject()

    fun saveVersionName() {
        val versionName = appData.versionName
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