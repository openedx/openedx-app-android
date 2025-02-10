package org.openedx.core.domain.model

data class CourseEnrollments(
    val enrollments: DashboardCourseList,
    val configs: AppConfig,
    val primary: EnrolledCourse?,
)
