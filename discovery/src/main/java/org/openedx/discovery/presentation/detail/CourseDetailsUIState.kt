package org.openedx.discovery.presentation.detail

import org.openedx.core.domain.model.Course

sealed class CourseDetailsUIState {
    data class CourseData(val course: Course, val isUserLoggedIn: Boolean = false) :
        CourseDetailsUIState()

    object Loading : CourseDetailsUIState()
}
