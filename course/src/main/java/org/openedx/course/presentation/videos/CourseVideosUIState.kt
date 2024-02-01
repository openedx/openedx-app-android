package org.openedx.course.presentation.videos

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState

sealed class CourseVideosUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val courseSubSections: Map<String, List<Block>>,
        val courseSectionsState: Map<String, Boolean>,
        val subSectionsDownloadsCount: Map<String, Int>,
        val allDownloadModulesState: AllDownloadModulesState
    ) : CourseVideosUIState()

    data class Empty(val message: String) : CourseVideosUIState()
    object Loading : CourseVideosUIState()
}

data class AllDownloadModulesState(
    val isAllBlocksDownloadedOrDownloading: Boolean,
    val remainingDownloadModelsCount: Int,
    val remainingDownloadModelsSize: Long,
    val allDownloadModelsCount: Int,
    val allDownloadModelsSize: Long
)
