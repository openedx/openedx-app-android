package org.openedx.core.domain.model

data class CourseList(
    val pagination: Pagination,
    val results: List<Course>,
)