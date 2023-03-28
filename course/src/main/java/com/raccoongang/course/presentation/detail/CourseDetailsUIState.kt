package com.raccoongang.course.presentation.detail

import com.raccoongang.core.domain.model.Course

sealed class CourseDetailsUIState {
    data class CourseData(val course: Course) : CourseDetailsUIState()
    object Loading : CourseDetailsUIState()
}