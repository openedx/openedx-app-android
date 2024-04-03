package org.openedx.discussion.presentation.topics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.DiscussionAnalytics

class DiscussionTopicsViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: DiscussionAnalytics,
    val courseId: String,
    val courseName: String,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DiscussionTopicsUIState>()
    val uiState: LiveData<DiscussionTopicsUIState>
        get() = _uiState

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _isUpdating = MutableSharedFlow<Boolean>()
    val isUpdating: SharedFlow<Boolean>
        get() = _isUpdating.asSharedFlow()

    init {
        getCourseTopics()
    }

    fun updateCourseTopics() {
        viewModelScope.launch {
            try {
                _isUpdating.emit(true)
                val response = interactor.getCourseTopics(courseId)
                _uiState.value = DiscussionTopicsUIState.Topics(response)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            }
            _isUpdating.emit(false)
        }
    }

    fun getCourseTopics() {
        _uiState.value = DiscussionTopicsUIState.Loading
        getCourseTopicsInternal()
    }

    private fun getCourseTopicsInternal() {
        viewModelScope.launch {
            try {
                val response = interactor.getCourseTopics(courseId)
                _uiState.value = DiscussionTopicsUIState.Topics(response)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            }
        }
    }

    fun discussionClickedEvent(action: String, data: String, title: String) {
        when (action) {
            DiscussionTopic.ALL_POSTS -> {
                analytics.discussionAllPostsClickedEvent(courseId, courseName)
            }

            DiscussionTopic.FOLLOWING_POSTS -> {
                analytics.discussionFollowingClickedEvent(courseId, courseName)
            }

            DiscussionTopic.TOPIC -> {
                analytics.discussionTopicClickedEvent(courseId, courseName, data, title)
            }
        }
    }
}