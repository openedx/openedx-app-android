package org.openedx.discovery.domain.model

import org.openedx.core.domain.model.Pagination

data class CourseList(
    val pagination: Pagination,
    val results: List<Course>,
)
