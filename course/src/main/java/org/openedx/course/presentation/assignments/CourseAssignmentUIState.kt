package org.openedx.course.presentation.assignments

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.Progress

sealed class CourseAssignmentUIState {
    data class CourseData(
        val groupedAssignments: Map<String, List<Block>>,
        val courseProgress: CourseProgress,
        val progress: Progress,
        val sectionNames: Map<String, String>
    ) : CourseAssignmentUIState()
    data object Empty : CourseAssignmentUIState()
    data object Loading : CourseAssignmentUIState()
}
