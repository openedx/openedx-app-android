package org.openedx.course.presentation.section

import org.openedx.core.domain.model.Block

sealed class CourseSectionUIState {
    data class Blocks(
        val blocks: List<Block>,
        val sectionName: String,
        val courseName: String
    ) : CourseSectionUIState()
    data object Loading : CourseSectionUIState()
}
