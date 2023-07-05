package org.openedx.course.presentation.section

import org.openedx.core.domain.model.Block
import org.openedx.core.module.db.DownloadedState

sealed class CourseSectionUIState {
    data class Blocks(
        val blocks: List<Block>,
        val downloadedState: Map<String, DownloadedState>,
        val courseName: String
    ) : CourseSectionUIState()
    object Loading : CourseSectionUIState()
}