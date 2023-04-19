package com.raccoongang.core.domain.model

data class DashboardCourseList(
    val pagination: Pagination,
    val courses: List<EnrolledCourse>
)
