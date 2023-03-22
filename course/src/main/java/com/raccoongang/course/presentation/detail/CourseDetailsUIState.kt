package com.raccoongang.course.presentation.detail

import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.EnrolledCourse


sealed class CourseDetailsUIState {
    data class CourseData(val course: Course, val enrolledCourse: EnrolledCourse?) : CourseDetailsUIState()
    object Loading : CourseDetailsUIState()
}