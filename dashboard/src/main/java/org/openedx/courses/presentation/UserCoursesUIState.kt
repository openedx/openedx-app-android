package org.openedx.courses.presentation

import org.openedx.courses.domain.model.UserCourses

sealed class UserCoursesUIState {
    data class Courses(val userCourses: UserCourses) : UserCoursesUIState()
    object Empty : UserCoursesUIState()
    object Loading : UserCoursesUIState()
}
