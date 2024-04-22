package org.openedx.discovery.presentation.search

import org.openedx.discovery.domain.model.Course

sealed class CourseSearchUIState {
    data class Courses(val courses: List<Course>, val numCourses: Int) : CourseSearchUIState()
    data object Loading : CourseSearchUIState()
}
