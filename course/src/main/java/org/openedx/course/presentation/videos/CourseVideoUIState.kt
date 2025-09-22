package org.openedx.course.presentation.videos

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.download.DownloadModelsSize
import org.openedx.core.utils.VideoPreview

sealed class CourseVideoUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val courseVideos: Map<String, List<Block>>,
        val subSectionsDownloadsCount: Map<String, Int>,
        val downloadModelsSize: DownloadModelsSize,
        val isCompletedSectionsShown: Boolean,
        val videoPreview: Map<String, VideoPreview?>,
        val videoProgress: Map<String, Float?>,
    ) : CourseVideoUIState()

    data object Empty : CourseVideoUIState()
    data object Loading : CourseVideoUIState()
}
