package org.openedx.core.presentation.dialog.appreview

import androidx.appcompat.app.AppCompatActivity
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.presentation.global.AppData

class AppReviewManager(
    private val activity: AppCompatActivity,
    private val reviewPreferences: InAppReviewPreferences,
    private val appData: AppData
) {
    var isDialogShowed = false

    fun tryToOpenRateDialog() {
        val supportFragmentManager = activity.supportFragmentManager
        if (!supportFragmentManager.isDestroyed) {
            isDialogShowed = true
            val currentVersionName = reviewPreferences.formatVersionName(appData.versionName)
            if (
                !reviewPreferences.wasPositiveRated
                && currentVersionName.minorVersion - 2 >= reviewPreferences.lastReviewVersion.minorVersion
                || currentVersionName.majorVersion - 1 >= reviewPreferences.lastReviewVersion.majorVersion
            ) {
                val dialog = RateDialogFragment.newInstance()
                dialog.show(
                    supportFragmentManager,
                    RateDialogFragment::class.simpleName
                )
            }
        }
    }
}