package org.openedx.course.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.settings.video.VideoQualityType
import org.openedx.course.presentation.handouts.HandoutsType

interface CourseRouter {

    fun navigateToNoAccess(
        fm: FragmentManager,
        title: String
    )

    fun navigateToCourseSubsections(
        fm: FragmentManager,
        courseId: String,
        subSectionId: String,
        unitId: String = "",
        componentId: String = "",
        mode: CourseViewMode
    )

    fun navigateToCourseContainer(
        fm: FragmentManager,
        courseId: String,
        unitId: String,
        componentId: String = "",
        mode: CourseViewMode
    )

    fun replaceCourseContainer(
        fm: FragmentManager,
        courseId: String,
        unitId: String,
        componentId: String = "",
        mode: CourseViewMode
    )

    fun navigateToFullScreenVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String,
        isPlaying: Boolean
    )

    fun navigateToFullScreenYoutubeVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String,
        isPlaying: Boolean
    )

    fun navigateToHandoutsWebView(
        fm: FragmentManager,
        courseId: String,
        type: HandoutsType
    )

    fun navigateToDownloadQueue(fm: FragmentManager, descendants: List<String> = arrayListOf())

    fun navigateToVideoQuality(fm: FragmentManager, videoQualityType: VideoQualityType)

    fun navigateToDiscover(fm: FragmentManager)
}
