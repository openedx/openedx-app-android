package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.ProgressDb

data class Progress(
    @SerializedName("assignments_completed")
    val assignmentsCompleted: Int?,
    @SerializedName("total_assignments_count")
    val totalAssignmentsCount: Int?
) {
    fun mapToDomain(): org.openedx.core.domain.model.Progress {
        return org.openedx.core.domain.model.Progress(
            assignmentsCompleted = assignmentsCompleted ?: 0,
            totalAssignmentsCount = totalAssignmentsCount ?: 0
        )
    }

    fun mapToRoomEntity() = ProgressDb(
        assignmentsCompleted = assignmentsCompleted ?: 0,
        totalAssignmentsCount = totalAssignmentsCount ?: 0
    )
}
