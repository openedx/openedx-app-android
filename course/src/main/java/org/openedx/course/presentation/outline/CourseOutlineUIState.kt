package org.openedx.course.presentation.outline

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState

sealed class CourseOutlineUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val resumeComponent: Block?,
        val courseSubSections: Map<String, List<Block>>,
        val courseSectionsState: Map<String, Boolean>,
        val subSectionsDownloadsCount: Map<String, Int>,
        val datesBannerInfo: CourseDatesBannerInfo,
        val isValuePropEnabled: Boolean
    ) : CourseOutlineUIState()

    data object Loading : CourseOutlineUIState()
}
