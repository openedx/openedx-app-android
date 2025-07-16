package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.AssignmentProgressDb
import org.openedx.core.domain.model.AssignmentProgress

data class AssignmentProgress(
    @SerializedName("assignment_type")
    val assignmentType: String?,
    @SerializedName("num_points_earned")
    val numPointsEarned: Float?,
    @SerializedName("num_points_possible")
    val numPointsPossible: Float?,
    @SerializedName("short_label")
    val label: String?
) {
    fun mapToDomain(displayName: String) = AssignmentProgress(
        assignmentType = assignmentType ?: "",
        numPointsEarned = numPointsEarned ?: 0f,
        numPointsPossible = numPointsPossible ?: 0f,
        label = label ?: displayName.take(5)
    )

    fun mapToRoomEntity() = AssignmentProgressDb(
        assignmentType = assignmentType,
        numPointsEarned = numPointsEarned,
        numPointsPossible = numPointsPossible,
        label = label
    )
}
