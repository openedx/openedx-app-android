package org.openedx.course.presentation.progress

import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure

sealed class CourseProgressUIState {
    data object Error : CourseProgressUIState()
    data object Loading : CourseProgressUIState()
    data class Data(
        val progress: CourseProgress,
        val courseStructure: CourseStructure?,
    ) : CourseProgressUIState()
}
