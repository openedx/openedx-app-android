package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse

data class AllEnrolledCoursesUIState(
    val courses: List<EnrolledCourse>? = null,
    val refreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val showProgress: Boolean = false,
)
