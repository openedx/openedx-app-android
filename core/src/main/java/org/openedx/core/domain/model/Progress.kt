package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Progress(
    val assignmentsCompleted: Int,
    val totalAssignmentsCount: Int,
) : Parcelable {

    @IgnoredOnParcel
    val value: Float
        get() = if (totalAssignmentsCount != 0) {
            assignmentsCompleted.toFloat() / totalAssignmentsCount.toFloat()
        } else {
            0f
        }

    companion object {
        val DEFAULT_PROGRESS = Progress(0, 0)
    }
}
