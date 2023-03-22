package com.raccoongang.course.presentation.videos

import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.module.db.DownloadedState

sealed class CourseVideosUIState {
    data class CourseData(
        val blocks: List<Block>,
        val downloadedState: Map<String, DownloadedState>,
    ) : CourseVideosUIState()

    data class Empty(val message: String) : CourseVideosUIState()
}