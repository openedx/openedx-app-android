package com.raccoongang.discovery.presentation.search

import com.raccoongang.core.domain.model.Course

sealed class CourseSearchUIState {
    data class Courses(val courses: List<Course>, val numCourses: Int) : CourseSearchUIState()
    object Loading : CourseSearchUIState()
}