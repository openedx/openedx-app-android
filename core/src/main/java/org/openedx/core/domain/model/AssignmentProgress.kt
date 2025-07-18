package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.openedx.core.extension.safeDivBy

@Parcelize
data class AssignmentProgress(
    val assignmentType: String,
    val numPointsEarned: Float,
    val numPointsPossible: Float,
    val label: String
) : Parcelable {

    @IgnoredOnParcel
    val value: Float = numPointsEarned.safeDivBy(numPointsPossible)

    override fun toString(): String {
        return "${numPointsEarned.toInt()}/${numPointsPossible.toInt()}"
    }
}
