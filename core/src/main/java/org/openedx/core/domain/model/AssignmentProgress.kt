package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AssignmentProgress(
    val assignmentType: String,
    val numPointsEarned: Float,
    val numPointsPossible: Float
) : Parcelable
