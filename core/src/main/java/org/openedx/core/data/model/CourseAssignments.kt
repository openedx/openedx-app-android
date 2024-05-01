package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.CourseAssignmentsDb

data class CourseAssignments(
    @SerializedName("future_assignments")
    val futureAssignments: List<CourseDateBlock>?,
    @SerializedName("past_assignments")
    val pastAssignments: List<CourseDateBlock>?
) {
    fun mapToDomain(): org.openedx.core.domain.model.CourseAssignments =
        org.openedx.core.domain.model.CourseAssignments(
            futureAssignments = futureAssignments?.map {
                it.mapToDomain()
            },
            pastAssignments = pastAssignments?.map {
                it.mapToDomain()
            }
        )

    fun mapToRoomEntity() = CourseAssignmentsDb(
        futureAssignments = futureAssignments?.map {
            it.mapToRoomEntity()
        },
        pastAssignments = pastAssignments?.map {
            it.mapToRoomEntity()
        }
    )
}
