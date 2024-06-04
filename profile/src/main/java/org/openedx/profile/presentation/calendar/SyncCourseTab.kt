package org.openedx.profile.presentation.calendar

import androidx.annotation.StringRes
import org.openedx.profile.R

enum class SyncCourseTab(
    @StringRes
    val title: Int
) {
    SYNCED(R.string.profile_synced),
    NOT_SYNCED(R.string.profile_not_synced)
}