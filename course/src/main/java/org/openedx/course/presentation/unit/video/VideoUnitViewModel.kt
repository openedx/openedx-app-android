package org.openedx.course.presentation.unit.video

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.AppDataConstants
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSubtitleLanguageChanged
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics
import subtitleFile.TimedTextObject

open class VideoUnitViewModel(
    val courseId: String,
    val videoUrl: String,
    val blockId: String,
    private val courseRepository: CourseRepository,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val transcriptManager: TranscriptManager,
    courseAnalytics: CourseAnalytics,
) : BaseVideoViewModel(courseId, courseAnalytics) {

    var transcripts = emptyMap<String, String>()
    var isPlaying = true
    var transcriptLanguage = AppDataConstants.defaultLocale.language ?: "en"
        private set

    var isDownloaded = false

    private val _currentVideoTime = MutableLiveData<Long>(0)
    val currentVideoTime: LiveData<Long>
        get() = _currentVideoTime

    var duration = 0L

    protected val isUpdatedMutable = MutableLiveData(true)
    val isUpdated: LiveData<Boolean>
        get() = isUpdatedMutable

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _transcriptObject = MutableLiveData<TimedTextObject?>()
    val transcriptObject: LiveData<TimedTextObject?>
        get() = _transcriptObject

    private var timeList: List<Long>? = emptyList()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private var isBlockAlreadyCompleted = false

    init {
        initVideoProgress()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is CourseVideoPositionChanged && videoUrl == it.videoUrl) {
                    isUpdatedMutable.value = false
                    _currentVideoTime.value = it.videoTime
                    saveVideoProgress()
                    isUpdatedMutable.value = true
                    isPlaying = it.isPlaying
                } else if (it is CourseSubtitleLanguageChanged) {
                    transcriptLanguage = it.value
                    _transcriptObject.value = null
                    downloadSubtitles()
                }
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        saveVideoProgress()
        super.onPause(owner)
    }

    private fun saveVideoProgress() {
        viewModelScope.launch {
            courseRepository.saveVideoProgress(
                blockId,
                videoUrl,
                _currentVideoTime.value ?: 0L,
                duration
            )
        }
    }

    fun downloadSubtitles() {
        viewModelScope.launch(Dispatchers.IO) {
            transcriptManager.downloadTranscriptsForVideo(getTranscriptUrl())?.let { result ->
                _transcriptObject.postValue(result)
                timeList = result.captions.values.toList()
                    .map { it.start.mseconds.toLong() }
            }
        }
    }

    private fun getTranscriptUrl(): String {
        val defaultTranscripts = transcripts[transcriptLanguage]
        return when {
            !defaultTranscripts.isNullOrEmpty() -> defaultTranscripts
            transcripts.values.isNotEmpty() -> {
                transcriptLanguage = transcripts.keys.first()
                transcripts[transcriptLanguage] ?: ""
            }

            else -> ""
        }
    }

    open fun markBlockCompleted(blockId: String, medium: String) {
        if (!isBlockAlreadyCompleted) {
            logLoadedCompletedEvent(videoUrl, false, getCurrentVideoTime(), medium)
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

    private fun initVideoProgress() {
        viewModelScope.launch {
            try {
                val videoProgress = courseRepository.getVideoProgress(blockId)
                _currentVideoTime.value = videoProgress.videoTime
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
