package org.openedx.courses.presentation

import org.openedx.core.domain.model.CourseEnrollments

sealed class PrimaryCourseUIState {
    data class Courses(val userCourses: CourseEnrollments) : PrimaryCourseUIState()
    data object Empty : PrimaryCourseUIState()
    data object Loading : PrimaryCourseUIState()
}
