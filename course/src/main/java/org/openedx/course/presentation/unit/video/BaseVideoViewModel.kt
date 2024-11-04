package org.openedx.course.presentation.unit.video

import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.foundation.presentation.BaseViewModel

open class BaseVideoViewModel(
    private val courseId: String,
    private val courseAnalytics: CourseAnalytics,
) : BaseViewModel() {

    fun logVideoSpeedEvent(videoUrl: String, speed: Float, currentVideoTime: Long, medium: String) {
        logVideoEvent(
            event = CourseAnalyticsEvent.VIDEO_CHANGE_SPEED,
            params = buildMap {
                put(CourseAnalyticsKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticsKey.SPEED.key, speed)
                put(CourseAnalyticsKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticsKey.PLAY_MEDIUM.key, medium)
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
            event = CourseAnalyticsEvent.VIDEO_SEEKED,
            params = buildMap {
                put(CourseAnalyticsKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticsKey.SKIP_INTERVAL.key, duration)
                put(CourseAnalyticsKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticsKey.PLAY_MEDIUM.key, medium)
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
            event = if (isLoaded) CourseAnalyticsEvent.VIDEO_LOADED else CourseAnalyticsEvent.VIDEO_COMPLETED,
            params = buildMap {
                put(CourseAnalyticsKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticsKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticsKey.PLAY_MEDIUM.key, medium)
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
            event = if (isPlaying) CourseAnalyticsEvent.VIDEO_PLAYED else CourseAnalyticsEvent.VIDEO_PAUSED,
            params = buildMap {
                put(CourseAnalyticsKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticsKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticsKey.PLAY_MEDIUM.key, medium)
            }
        )
    }

    private fun logVideoEvent(event: CourseAnalyticsEvent, params: Map<String, Any?>) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COMPONENT.key, CourseAnalyticsKey.VIDEO_PLAYER.key)
                putAll(params)
            }
        )
    }

    fun logCastConnection(event: CourseAnalyticsEvent) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.PLAY_MEDIUM.key, CourseAnalyticsKey.GOOGLE_CAST.key)
            }
        )
    }
}
