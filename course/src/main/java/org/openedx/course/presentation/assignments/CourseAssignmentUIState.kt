package org.openedx.course.presentation.assignments

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.Progress

sealed class CourseAssignmentUIState {
    data class CourseData(
        val groupedAssignments: Map<String, List<Block>>,
        val progress: Progress
    ) : CourseAssignmentUIState()

    data object Error : CourseAssignmentUIState()
    data object Loading : CourseAssignmentUIState()
} 