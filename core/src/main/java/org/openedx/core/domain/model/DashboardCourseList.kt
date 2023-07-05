package org.openedx.core.domain.model

data class DashboardCourseList(
    val pagination: Pagination,
    val courses: List<EnrolledCourse>
)
