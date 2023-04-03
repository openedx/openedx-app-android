package com.raccoongang.course.presentation.outline

import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.module.db.DownloadedState

sealed class CourseOutlineUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val resumeBlock: Block?
    ) : CourseOutlineUIState()

    object Loading : CourseOutlineUIState()
}
