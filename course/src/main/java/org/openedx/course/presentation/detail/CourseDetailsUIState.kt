package org.openedx.course.presentation.detail

import org.openedx.core.domain.model.Course

sealed class CourseDetailsUIState {
    data class CourseData(val course: Course) : CourseDetailsUIState()
    object Loading : CourseDetailsUIState()
}