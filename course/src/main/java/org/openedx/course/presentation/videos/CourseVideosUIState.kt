package org.openedx.course.presentation.videos

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.download.DownloadModelsSize

sealed class CourseVideosUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val courseSubSections: Map<String, List<Block>>,
        val courseSectionsState: Map<String, Boolean>,
        val subSectionsDownloadsCount: Map<String, Int>,
        val downloadModelsSize: DownloadModelsSize,
        val useRelativeDates: Boolean
    ) : CourseVideosUIState()

    data object Empty : CourseVideosUIState()
    data object Loading : CourseVideosUIState()
}
