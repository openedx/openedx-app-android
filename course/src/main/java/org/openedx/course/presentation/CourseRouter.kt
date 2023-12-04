package org.openedx.course.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.course.presentation.handouts.HandoutsType
import java.util.Date

interface CourseRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String
    )

    fun navigateToNoAccess(
        fm: FragmentManager,
        title: String,
        coursewareAccess: CoursewareAccess,
        auditAccessExpires: Date?
    )

    fun navigateToCourseSubsections(
        fm: FragmentManager,
        courseId: String,
        blockId: String,
        mode: CourseViewMode,
        descendantId: String? = ""
    )

    fun navigateToCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
        mode: CourseViewMode
    )

    fun replaceCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
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
        title: String,
        type: HandoutsType
    )

}