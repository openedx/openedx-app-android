package org.openedx.course.presentation.videos

import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState

sealed class CourseVideosUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
    ) : CourseVideosUIState()

    data class Empty(val message: String) : CourseVideosUIState()
    object Loading : CourseVideosUIState()
}