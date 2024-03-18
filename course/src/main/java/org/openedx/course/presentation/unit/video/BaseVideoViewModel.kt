package org.openedx.course.presentation.unit.video

import org.openedx.core.BaseViewModel
import org.openedx.course.presentation.CourseAnalyticKey
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent

open class BaseVideoViewModel(
    private val courseId: String,
    private val courseAnalytics: CourseAnalytics,
) : BaseViewModel() {

    fun logVideoSpeedEvent(videoUrl: String, speed: Float, currentVideoTime: Long, medium: String) {
        logVideoEvent(
            CourseAnalyticsEvent.VIDEO_CHANGE_SPEED,
            buildMap {
                put(CourseAnalyticKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticKey.SPEED.key, speed)
                put(CourseAnalyticKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticKey.PLAY_MEDIUM.key, medium)
            }
        )
    }

    fun logVideoSeekEvent(
        videoUrl: String,
        duration: Long,
        currentVideoTime: Long,
        medium: String,
    ) {
        logVideoEvent(
            CourseAnalyticsEvent.VIDEO_SEEKED,
            buildMap {
                put(CourseAnalyticKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticKey.SKIP_INTERVAL.key, duration)
                put(CourseAnalyticKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticKey.PLAY_MEDIUM.key, medium)
            }
        )
    }

    fun logLoadedCompletedEvent(
        videoUrl: String,
        isLoaded: Boolean,
        currentVideoTime: Long,
        medium: String,
    ) {
        logVideoEvent(
            if (isLoaded) CourseAnalyticsEvent.VIDEO_LOADED else CourseAnalyticsEvent.VIDEO_COMPLETED,
            buildMap {
                put(CourseAnalyticKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticKey.PLAY_MEDIUM.key, medium)
            }
        )
    }

    fun logPlayPauseEvent(
        videoUrl: String,
        isPlaying: Boolean,
        currentVideoTime: Long,
        medium: String,
    ) {
        logVideoEvent(
            if (isPlaying) CourseAnalyticsEvent.VIDEO_PLAYED else CourseAnalyticsEvent.VIDEO_PAUSED,
            buildMap {
                put(CourseAnalyticKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticKey.PLAY_MEDIUM.key, medium)
            }
        )
    }

    private fun logVideoEvent(event: CourseAnalyticsEvent, params: Map<String, Any?>) {
        courseAnalytics.logEvent(
            event.eventName,
            buildMap {
                put(CourseAnalyticKey.NAME.key, event.biValue)
                put(CourseAnalyticKey.COURSE_ID.key, courseId)
                put(CourseAnalyticKey.COMPONENT.key, CourseAnalyticKey.VIDEO_PLAYER.key)
                putAll(params)
            })
    }

    fun logCastConnection(event: CourseAnalyticsEvent) {
        courseAnalytics.logEvent(
            event = event.eventName,
            buildMap {
                put(CourseAnalyticKey.NAME.key, event.biValue)
                put(CourseAnalyticKey.PLAY_MEDIUM.key, CourseAnalyticKey.GOOGLE_CAST.key)
            }
        )
    }
}
