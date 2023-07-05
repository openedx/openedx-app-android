package org.openedx.discussion.presentation.topics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment.Companion.ALL_POSTS
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment.Companion.FOLLOWING_POSTS
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment.Companion.TOPIC
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