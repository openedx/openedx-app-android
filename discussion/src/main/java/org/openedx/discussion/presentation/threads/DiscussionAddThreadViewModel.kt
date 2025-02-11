package org.openedx.discussion.presentation.threads

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class DiscussionAddThreadViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    private val courseId: String
) : BaseViewModel() {

    private val _newThread = MutableLiveData<org.openedx.discussion.domain.model.Thread>()
    val newThread: LiveData<org.openedx.discussion.domain.model.Thread>
        get() = _newThread

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun createThread(
        topicId: String,
        type: String,
        title: String,
        rawBody: String,
        follow: Boolean
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _newThread.value = interactor.createThread(topicId, courseId, type, title, rawBody, follow)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
            _isLoading.value = false
        }
    }

    private fun getCachedTopics() = interactor.getCachedTopics(courseId)

    fun getHandledTopics(): List<Pair<String, String>> {
        val topics = getCachedTopics().filterNot { it.id == "" }
        return topics.map { Pair(it.name, it.id) }
    }

    fun getHandledTopicById(topicId: String): Pair<String, String> {
        val topics = getHandledTopics()
        return topics.find { it.second == topicId } ?: topics.firstOrNull() ?: Pair("", "")
    }

    fun sendThreadAdded() {
        viewModelScope.launch {
            notifier.send(DiscussionThreadAdded())
        }
    }
}
