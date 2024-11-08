package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseEnrollmentDetails as DomainCourseEnrollmentDetails

data class CourseEnrollmentDetails(
    @SerializedName("id")
    val id: String,
    @SerializedName("course_updates")
    val courseUpdates: String?,
    @SerializedName("course_handouts")
    val courseHandouts: String?,
    @SerializedName("discussion_url")
    val discussionUrl: String?,
    @SerializedName("course_access_details")
    val courseAccessDetails: CourseAccessDetails,
    @SerializedName("certificate")
    val certificate: Certificate?,
    @SerializedName("enrollment_details")
    val enrollmentDetails: EnrollmentDetails,
    @SerializedName("course_info_overview")
    val courseInfoOverview: CourseInfoOverview,
) {
    fun mapToDomain(): DomainCourseEnrollmentDetails {
        return DomainCourseEnrollmentDetails(
            id = id,
            courseUpdates = courseUpdates ?: "",
            courseHandouts = courseHandouts ?: "",
            discussionUrl = discussionUrl ?: "",
            courseAccessDetails = courseAccessDetails.mapToDomain(),
            certificate = certificate?.mapToDomain(),
            enrollmentDetails = enrollmentDetails.mapToDomain(),
            courseInfoOverview = courseInfoOverview.mapToDomain(),
        )
    }
}
