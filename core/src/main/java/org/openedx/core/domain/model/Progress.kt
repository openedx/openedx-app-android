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
    val value: Float = try {
        assignmentsCompleted.toFloat() / totalAssignmentsCount.toFloat()
    } catch (_: ArithmeticException) {
        0f
    }

    companion object {
        val DEFAULT_PROGRESS = Progress(0, 0)
    }
}
