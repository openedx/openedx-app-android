package org.openedx.course.presentation.unit.video

import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import org.openedx.core.BaseViewModel
import org.openedx.course.data.repository.CourseRepository
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences

class VideoViewModel(
    private val courseId: String,
    private val courseRepository: CourseRepository,
    private val notifier: CourseNotifier,
    private val preferencesManager: CorePreferences
) : BaseViewModel() {

    var videoUrl = ""
    var currentVideoTime = 0L
    var isPlaying: Boolean? = null

    private var isBlockAlreadyCompleted = false


    fun sendTime() {
        if (currentVideoTime != C.TIME_UNSET) {
            viewModelScope.launch {
                notifier.send(CourseVideoPositionChanged(videoUrl, currentVideoTime, isPlaying ?: false))
            }
        }
    }

    fun markBlockCompleted(blockId: String) {
        if (!isBlockAlreadyCompleted) {
            viewModelScope.launch {
                try {
                    isBlockAlreadyCompleted = true
                    courseRepository.markBlocksCompletion(
                        courseId,
                        listOf(blockId)
                    )
                } catch (e: Exception) {
                    isBlockAlreadyCompleted = false
                }
            }
        }
    }

    fun getVideoQuality() = preferencesManager.videoSettings.videoQuality
}