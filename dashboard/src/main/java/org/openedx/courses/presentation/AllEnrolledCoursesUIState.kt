package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse

sealed class AllEnrolledCoursesUIState {
    data class Courses(val courses: List<EnrolledCourse>) : AllEnrolledCoursesUIState()
    data object Empty : AllEnrolledCoursesUIState()
    data object Loading : AllEnrolledCoursesUIState()
}
