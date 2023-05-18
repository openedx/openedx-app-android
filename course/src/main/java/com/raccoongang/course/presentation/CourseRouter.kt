package com.raccoongang.course.presentation

import androidx.fragment.app.FragmentManager
import com.raccoongang.core.domain.model.CoursewareAccess
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.course.presentation.handouts.HandoutsType
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

    fun navigateToCourseUnits(
        fm: FragmentManager,
        courseId: String,
        blockId: String,
        courseName: String,
        mode: CourseViewMode,
    )

    fun navigateToCourseSubsections(
        fm: FragmentManager,
        courseId: String,
        blockId: String,
        title: String,
        mode: CourseViewMode,
    )

    fun navigateToCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
        courseName: String,
        mode: CourseViewMode
    )

    fun replaceCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
        courseName: String,
        mode: CourseViewMode
    )

    fun navigateToFullScreenVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String
    )

    fun navigateToFullScreenYoutubeVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String
    )

    fun navigateToHandoutsWebView(
        fm: FragmentManager,
        courseId: String,
        title: String,
        type: HandoutsType
    )

}