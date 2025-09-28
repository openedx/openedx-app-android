package org.openedx.course.presentation.unit.video

import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics

class VideoViewModel(
    private val courseId: String,
    private val courseRepository: CourseRepository,
    private val notifier: CourseNotifier,
    private val preferencesManager: CorePreferences,
    courseAnalytics: CourseAnalytics,
) : BaseVideoViewModel(courseId, courseAnalytics) {

    var videoUrl = ""
    var currentVideoTime = 0L
    var duration = 0L
    var isPlaying: Boolean? = null

    private var isBlockAlreadyCompleted = false

    fun sendTime() {
        if (currentVideoTime != C.TIME_UNSET) {
            viewModelScope.launch {
                notifier.send(
                    CourseVideoPositionChanged(
                        videoUrl,
                        currentVideoTime,
                        duration,
                        isPlaying == true
                    )
                )
            }
        }
    }

    fun markBlockCompleted(blockId: String, medium: String) {
        if (!isBlockAlreadyCompleted) {
            logLoadedCompletedEvent(videoUrl, false, currentVideoTime, medium)
            viewModelScope.launch {
                try {
                    isBlockAlreadyCompleted = true
                    courseRepository.markBlocksCompletion(
                        courseId,
                        listOf(blockId)
                    )
                    notifier.send(CourseCompletionSet())
                } catch (e: Exception) {
                    e.printStackTrace()
                    isBlockAlreadyCompleted = false
                }
            }
        }
    }

    fun getVideoQuality() = preferencesManager.videoSettings.videoStreamingQuality
}
