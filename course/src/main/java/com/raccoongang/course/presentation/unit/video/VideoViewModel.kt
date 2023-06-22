package com.raccoongang.course.presentation.unit.video

import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.C
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.course.data.repository.CourseRepository
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseVideoPositionChanged
import kotlinx.coroutines.launch

class VideoViewModel(
    private val courseId: String,
    private val courseRepository: CourseRepository,
    private val preferencesManager: PreferencesManager,
    private val notifier: CourseNotifier
) : BaseViewModel() {

    var videoUrl = ""
    var currentVideoTime = 0L

    private var isBlockAlreadyCompleted = false


    fun sendTime() {
        if (currentVideoTime != C.TIME_UNSET) {
            viewModelScope.launch {
                notifier.send(CourseVideoPositionChanged(videoUrl, currentVideoTime))
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
                    e.printStackTrace()
                }
            }
        }
    }

}