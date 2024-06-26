package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.EnrollmentStatus

data class EnrollmentStatus(
    @SerializedName("course_id")
    val courseId: String?,
    @SerializedName("course_name")
    val courseName: String?,
    @SerializedName("is_active")
    val isActive: Boolean?
) {
    fun mapToDomain() = EnrollmentStatus(
        courseId = courseId ?: "",
        courseName = courseName ?: "",
        isActive = isActive ?: false
    )
}
