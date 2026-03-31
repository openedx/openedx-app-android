package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseDate as DomainCourseDate
import org.openedx.core.domain.model.CourseDatesResponse as DomainCourseDatesResponse

data class CourseDate(
    @SerializedName("course_id")
    val courseId: String,
    @SerializedName("first_component_block_id")
    val firstComponentBlockId: String?,
    @SerializedName("due_date")
    val dueDate: String?,
    @SerializedName("assignment_title")
    val assignmentTitle: String?,
    @SerializedName("learner_has_access")
    val learnerHasAccess: Boolean?,
    @SerializedName("relative")
    val relative: Boolean?,
    @SerializedName("course_name")
    val courseName: String?
) {
    fun mapToDomain(): DomainCourseDate? {
        val dueDate = TimeUtils.iso8601ToDate(dueDate ?: "")
        return DomainCourseDate(
            courseId = courseId,
            firstComponentBlockId = firstComponentBlockId ?: "",
            dueDate = dueDate ?: return null,
            assignmentTitle = assignmentTitle ?: "",
            learnerHasAccess = learnerHasAccess ?: false,
            courseName = courseName ?: "",
            relative = relative ?: false
        )
    }
}

data class CourseDatesResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
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
