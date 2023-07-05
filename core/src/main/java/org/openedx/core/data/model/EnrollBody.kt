package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class EnrollBody(
    @SerializedName("course_details")
    val courseDetails: CourseDetails
) {
    data class CourseDetails(
        @SerializedName("course_id")
        val courseId: String,
        @SerializedName("email_opt_in")
        val emailOptIn: String?,
    )
}
