package org.openedx.course.presentation.unit.video

import org.openedx.core.BaseViewModel
import org.openedx.course.presentation.CourseAnalyticEvent
import org.openedx.course.presentation.CourseAnalyticKey
import org.openedx.course.presentation.CourseAnalyticValue
import org.openedx.course.presentation.CourseAnalytics

open class BaseVideoViewModel(
    private val courseId: String,
    private val courseAnalytics: CourseAnalytics,
) : BaseViewModel() {

    fun logVideoSpeedEvent(videoUrl: String, speed: Float, currentVideoTime: Long, medium: String) {
        logVideoEvent(
            CourseAnalyticEvent.VIDEO_CHANGE_SPEED.event,
            buildMap {
                put(CourseAnalyticKey.NAME.key, CourseAnalyticValue.VIDEO_CHANGE_SPEED.biValue)
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
            CourseAnalyticEvent.VIDEO_SEEKED.event,
            buildMap {
                put(CourseAnalyticKey.NAME.key, CourseAnalyticValue.VIDEO_SEEKED.biValue)
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
            if (isLoaded) CourseAnalyticEvent.VIDEO_LOADED.event else CourseAnalyticEvent.VIDEO_COMPLETED.event,
            buildMap {
                put(
                    CourseAnalyticKey.NAME.key,
                    if (isLoaded) CourseAnalyticValue.VIDEO_LOADED.biValue
                    else CourseAnalyticValue.VIDEO_COMPLETED.biValue
                )
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
            if (isPlaying) CourseAnalyticEvent.VIDEO_PLAYED.event else CourseAnalyticEvent.VIDEO_PAUSED.event,
            buildMap {
                put(
                    CourseAnalyticKey.NAME.key,
                    if (isPlaying) CourseAnalyticValue.VIDEO_PLAYED.biValue
                    else CourseAnalyticValue.VIDEO_PAUSED.biValue
                )
                put(CourseAnalyticKey.OPEN_IN_BROWSER.key, videoUrl)
                put(CourseAnalyticKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticKey.PLAY_MEDIUM.key, medium)
            }
        )
    }

    private fun logVideoEvent(event: String, params: Map<String, Any?>) {
        courseAnalytics.logEvent(event, buildMap {
            put(CourseAnalyticKey.COURSE_ID.key, courseId)
            put(CourseAnalyticKey.COMPONENT.key, CourseAnalyticKey.VIDEO_PLAYER.key)
            putAll(params)
        })
    }

    fun logCastConnection(isConnected: Boolean) {
        if (isConnected) {
            courseAnalytics.logEvent(
                event = CourseAnalyticEvent.CAST_CONNECTED.event,
                buildMap {
                    put(CourseAnalyticKey.NAME.key, CourseAnalyticValue.CAST_CONNECTED.biValue)
                    put(CourseAnalyticKey.PLAY_MEDIUM.key, CourseAnalyticValue.GOOGLE_CAST.biValue)
                }
            )
        } else {
            courseAnalytics.logEvent(
                event = CourseAnalyticEvent.CAST_DISCONNECTED.event,
                buildMap {
                    put(CourseAnalyticKey.NAME.key, CourseAnalyticValue.CAST_DISCONNECTED.biValue)
                    put(CourseAnalyticKey.PLAY_MEDIUM.key, CourseAnalyticValue.GOOGLE_CAST.biValue)
                }
            )
        }
    }
}
