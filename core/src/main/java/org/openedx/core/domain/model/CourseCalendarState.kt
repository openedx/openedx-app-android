package org.openedx.core.domain.model

data class CourseCalendarState(
    val checksum: Int,
    val courseId: String,
    val isCourseSyncEnabled: Boolean
)
