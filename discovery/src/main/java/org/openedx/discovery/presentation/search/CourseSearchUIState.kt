package org.openedx.discovery.presentation.search

import org.openedx.core.domain.model.Course

sealed class CourseSearchUIState {
    data class Courses(val courses: List<Course>, val numCourses: Int) : CourseSearchUIState()
    object Loading : CourseSearchUIState()
}