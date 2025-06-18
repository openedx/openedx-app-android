package org.openedx.course.presentation.progress

import org.openedx.core.domain.model.CourseProgress

sealed class CourseProgressUIState {
    data object Error : CourseProgressUIState()
    data object Loading : CourseProgressUIState()
    data class Data(val progress: CourseProgress) : CourseProgressUIState()
}
