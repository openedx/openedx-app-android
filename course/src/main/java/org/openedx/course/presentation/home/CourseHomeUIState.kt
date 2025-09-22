package org.openedx.course.presentation.home

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.utils.VideoPreview

sealed class CourseHomeUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val courseProgress: CourseProgress?,
        val next: Pair<Block, Block>?, // section and subsection, nullable
        val downloadedState: Map<String, DownloadedState>,
        val resumeComponent: Block?,
        val resumeUnitTitle: String,
        val courseSubSections: Map<String, List<Block>>,
        val subSectionsDownloadsCount: Map<String, Int>,
        val datesBannerInfo: CourseDatesBannerInfo,
        val useRelativeDates: Boolean,
        val courseVideos: Map<String, List<Block>>,
        val courseAssignments: List<Block>,
        val videoPreview: VideoPreview?,
        val videoProgress: Float?,
    ) : CourseHomeUIState()

    data object Error : CourseHomeUIState()
    data object Loading : CourseHomeUIState()
    data object Waiting : CourseHomeUIState()
}
