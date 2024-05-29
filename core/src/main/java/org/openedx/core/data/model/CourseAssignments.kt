package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.CourseAssignmentsDb
import org.openedx.core.domain.model.CourseAssignments

data class CourseAssignments(
    @SerializedName("future_assignments")
    val futureAssignments: List<CourseDateBlock>?,
    @SerializedName("past_assignments")
    val pastAssignments: List<CourseDateBlock>?,
) {
    fun mapToDomain() = CourseAssignments(
        futureAssignments = futureAssignments?.mapNotNull {
            it.mapToDomain()
        },
        pastAssignments = pastAssignments?.mapNotNull {
            it.mapToDomain()
        }
    )

    fun mapToRoomEntity() = CourseAssignmentsDb(
        futureAssignments = futureAssignments?.mapNotNull {
            it.mapToRoomEntity()
        },
        pastAssignments = pastAssignments?.mapNotNull {
            it.mapToRoomEntity()
        }
    )
}
