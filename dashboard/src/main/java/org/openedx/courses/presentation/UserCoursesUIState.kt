package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse

sealed class UserCoursesUIState {
    data class Courses(val courses: List<EnrolledCourse>) : UserCoursesUIState()
    object Empty : UserCoursesUIState()
    object Loading : UserCoursesUIState()
}
