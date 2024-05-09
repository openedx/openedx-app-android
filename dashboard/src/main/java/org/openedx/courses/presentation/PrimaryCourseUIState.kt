package org.openedx.courses.presentation

import org.openedx.courses.domain.model.UserCourses

sealed class PrimaryCourseUIState {
    data class Courses(val userCourses: UserCourses) : PrimaryCourseUIState()
    data object Empty : PrimaryCourseUIState()
    data object Loading : PrimaryCourseUIState()
}
