package org.openedx.discussion.presentation.threads

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager

class DiscussionAddThreadViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    private val courseId: String
) : BaseViewModel(resourceManager) {

    private val _newThread = MutableLiveData<Thread>()
    val newThread: LiveData<Thread>
        get() = _newThread

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
                handleErrorUiMessage(
                    throwable = e,
                )
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
