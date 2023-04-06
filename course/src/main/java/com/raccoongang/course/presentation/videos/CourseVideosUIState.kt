package com.raccoongang.course.presentation.videos

import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.module.db.DownloadedState

sealed class CourseVideosUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
    ) : CourseVideosUIState()

    data class Empty(val message: String) : CourseVideosUIState()
    object Loading : CourseVideosUIState()
}