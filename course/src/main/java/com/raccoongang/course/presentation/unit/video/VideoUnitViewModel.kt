package com.raccoongang.course.presentation.unit.video

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.module.TranscriptManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CoursePauseVideo
import com.raccoongang.core.system.notifier.CourseVideoPositionChanged
import com.raccoongang.course.data.repository.CourseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import subtitleFile.TimedTextObject

class VideoUnitViewModel(
    val courseId: String,
    private val courseRepository: CourseRepository,
    private val preferencesManager: PreferencesManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val transcriptManager: TranscriptManager
) : BaseViewModel() {

    var videoUrl = ""
    var transcriptUrl = ""

    private val _currentVideoTime = MutableLiveData<Long>(0)
    val currentVideoTime: LiveData<Long>
        get() = _currentVideoTime

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

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _transcriptObject = MutableLiveData<TimedTextObject?>()
    val transcriptObject: LiveData<TimedTextObject?>
        get() = _transcriptObject

    private var timeList: List<Long>? = emptyList()

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
                    _currentVideoTime.value = it.videoTime
                    _isUpdated.value = true
                } else if (it is CoursePauseVideo) {
                    _isVideoPaused.value = true
                }
            }
        }
    }

    fun downloadSubtitles() {
        viewModelScope.launch {
            transcriptManager.downloadTranscriptsForVideo(transcriptUrl)?.let { result ->
                _transcriptObject.value = result
                timeList = result.captions.values.toList()
                    .map { it.start.mseconds.toLong() }
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

    fun setCurrentVideoTime(value: Long) {
        _currentVideoTime.value = value
        timeList?.let {
            val index = it.indexOfLast { subtitleTime ->
                subtitleTime < value
            }
            if (index != currentIndex.value) {
                _currentIndex.value = index
            }
        }
    }

    fun getCurrentVideoTime() = currentVideoTime.value ?: 0

}