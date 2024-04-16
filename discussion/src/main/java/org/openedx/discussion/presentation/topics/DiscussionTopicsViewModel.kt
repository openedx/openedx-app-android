package org.openedx.discussion.presentation.topics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment.Companion.ALL_POSTS
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment.Companion.FOLLOWING_POSTS
import org.openedx.discussion.presentation.topics.DiscussionTopicsFragment.Companion.TOPIC

class DiscussionTopicsViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val analytics: DiscussionAnalytics,
    private val networkConnection: NetworkConnection,
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

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    var courseName = ""

    fun updateCourseTopics(withSwipeRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (withSwipeRefresh) {
                    _isUpdating.value = true
                } else {
                    _uiState.value = DiscussionTopicsUIState.Loading
                }

                val response = interactor.getCourseTopics(courseId)
                _uiState.value = DiscussionTopicsUIState.Topics(response)
            } catch (e: Exception) {
                val errorMessage = if (e.isInternetError()) {
                    resourceManager.getString(R.string.core_error_no_connection)
                } else {
                    resourceManager.getString(R.string.core_error_unknown_error)
                }
                _uiMessage.value = UIMessage.SnackBarMessage(errorMessage)
            } finally {
                _isUpdating.value = false
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