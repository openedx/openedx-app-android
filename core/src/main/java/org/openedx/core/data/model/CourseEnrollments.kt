package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class CourseEnrollments(
    @SerializedName("enrollments")
    val enrollments: DashboardCourseList
)