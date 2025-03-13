package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseDate as DomainCourseDate
import org.openedx.core.domain.model.CourseDatesResponse as DomainCourseDatesResponse

data class CourseDate(
    @SerializedName("course_id")
    val courseId: String,
    @SerializedName("assignment_block_id")
    val assignmentBlockId: String,
    @SerializedName("due_date")
    val dueDate: String?,
    @SerializedName("assignment_title")
    val assignmentTitle: String?,
    @SerializedName("learner_has_access")
    val learnerHasAccess: Boolean?,
    @SerializedName("course_name")
    val courseName: String?
) {
    fun mapToDomain(): DomainCourseDate? {
        val dueDate = TimeUtils.iso8601ToDate(dueDate ?: "")
        return DomainCourseDate(
            courseId = courseId,
            assignmentBlockId = assignmentBlockId,
            dueDate = dueDate ?: return null,
            assignmentTitle = assignmentTitle ?: "",
            learnerHasAccess = learnerHasAccess ?: false,
            courseName = courseName ?: ""
        )
    }
}

data class CourseDatesResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: Int?,
    @SerializedName("previous")
    val previous: Int?,
    @SerializedName("results")
    val results: List<CourseDate>
) {
    fun mapToDomain(): DomainCourseDatesResponse {
        return DomainCourseDatesResponse(
            count = count,
            next = next,
            previous = previous,
            results = results.mapNotNull { it.mapToDomain() }
        )
    }
}
