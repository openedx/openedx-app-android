package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.EnrollmentStatus

data class EnrollmentStatus(
    @SerializedName("course_id")
    val courseId: String?,
    @SerializedName("course_name")
    val courseName: String?,
    @SerializedName("recently_active")
    val recentlyActive: Boolean?
) {
    fun mapToDomain() = EnrollmentStatus(
        courseId = courseId ?: "",
        courseName = courseName ?: "",
        recentlyActive = recentlyActive ?: false
    )
}
