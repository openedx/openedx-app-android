package org.openedx.courses.presentation

import org.openedx.courses.domain.model.UserCourses

sealed class UserCoursesUIState {
    data class Courses(val userCourses: UserCourses) : UserCoursesUIState()
    data object Empty : UserCoursesUIState()
    data object Loading : UserCoursesUIState()
}
