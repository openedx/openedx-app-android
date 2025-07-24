package org.openedx.course.presentation.home

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState

sealed class CourseHomeUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val resumeComponent: Block?,
        val resumeUnitTitle: String,
        val courseSubSections: Map<String, List<Block>>,
        val courseSectionsState: Map<String, Boolean>,
        val subSectionsDownloadsCount: Map<String, Int>,
        val datesBannerInfo: CourseDatesBannerInfo,
        val useRelativeDates: Boolean,
    ) : CourseHomeUIState()

    data object Error : CourseHomeUIState()
    data object Loading : CourseHomeUIState()
}
