package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Progress(
    val numPointsEarned: Int,
    val numPointsPossible: Int,
) : Parcelable {
    companion object {
        val DEFAULT_PROGRESS = Progress(0,0)
    }
}
