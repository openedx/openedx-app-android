package com.raccoongang.discussion.presentation.topics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.discussion.domain.interactor.DiscussionInteractor
import com.raccoongang.discussion.presentation.DiscussionAnalytics
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment.Companion.ALL_POSTS
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment.Companion.FOLLOWING_POSTS
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment.Companion.TOPIC
import kotlinx.coroutines.launch

class DiscussionTopicsViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: DiscussionAnalytics,
    val courseId: String
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DiscussionTopicsUIState>()
    val uiState: LiveData<DiscussionTopicsUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    var courseName = ""

    fun updateCourseTopics() {
        viewModelScope.launch {
            try {
                _isUpdating.value = true
                val response = interactor.getCourseTopics(courseId)
                _uiState.value = DiscussionTopicsUIState.Topics(response)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
            _isUpdating.value = false
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
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun discussionClickedEvent(action: String, data: String, title: String) {
        when (action) {
            ALL_POSTS -> {
                analytics.discussionAllPostsClickedEvent(courseId, courseName)
            }

            FOLLOWING_POSTS -> {
                analytics.discussionFollowingClickedEvent(courseId, courseName)
            }

            TOPIC -> {
                analytics.discussionTopicClickedEvent(courseId, courseName, data, title)
            }
        }
    }
}