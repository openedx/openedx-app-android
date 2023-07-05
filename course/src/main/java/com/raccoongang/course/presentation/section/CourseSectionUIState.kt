package com.raccoongang.course.presentation.section

import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.module.db.DownloadedState

sealed class CourseSectionUIState {
    data class Blocks(
        val blocks: List<Block>,
        val downloadedState: Map<String, DownloadedState>,
        val courseName: String
    ) : CourseSectionUIState()
    object Loading : CourseSectionUIState()
}