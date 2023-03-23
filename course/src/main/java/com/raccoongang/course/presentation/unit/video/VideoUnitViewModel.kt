package com.raccoongang.course.presentation.unit.video

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CoursePauseVideo
import com.raccoongang.core.system.notifier.CourseVideoPositionChanged
import com.raccoongang.course.data.repository.CourseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoUnitViewModel(
    val courseId: String,
    private val courseRepository: CourseRepository,
    private val preferencesManager: PreferencesManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection
) : BaseViewModel() {

    var videoUrl = ""
    var currentVideoTime = 0L

    var fullscreenHandled = false

    var isDownloaded = false

    private val _isUpdated = MutableLiveData(true)
    val isUpdated: LiveData<Boolean>
        get() = _isUpdated

    private val _isPopUpViewShow = MutableLiveData(true)
    val isPopUpViewShow: LiveData<Boolean>
        get() = _isPopUpViewShow

    private val _isVideoPaused = MutableLiveData<Boolean>()
    val isVideoPaused: LiveData<Boolean>
        get() = _isVideoPaused

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        viewModelScope.launch {
            delay(4000)
            _isPopUpViewShow.value = false
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is CourseVideoPositionChanged && videoUrl == it.videoUrl) {
                    _isUpdated.value = false
                    currentVideoTime = it.videoTime
                    _isUpdated.value = true
                } else if (it is CoursePauseVideo) {
                    _isVideoPaused.value = true
                }
            }
        }
    }

    fun markBlockCompleted(blockId: String) {
        viewModelScope.launch {
            try {
                courseRepository.markBlocksCompletion(
                    courseId,
                    listOf(blockId)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}