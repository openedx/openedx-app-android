package com.raccoongang.discussion.presentation.threads

import androidx.lifecycle.LifecycleOwner
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
import com.raccoongang.discussion.presentation.topics.DiscussionTopicsFragment
import com.raccoongang.discussion.system.notifier.DiscussionNotifier
import com.raccoongang.discussion.system.notifier.DiscussionThreadAdded
import com.raccoongang.discussion.system.notifier.DiscussionThreadDataChanged
import kotlinx.coroutines.launch

class DiscussionThreadsViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    val courseId: String,
    private val threadType: String
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DiscussionThreadsUIState>()
    val uiState: LiveData<DiscussionThreadsUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val threadsList = mutableListOf<com.raccoongang.discussion.domain.model.Thread>()
    private var nextPage = 1
    private var isLoading = false

    var topicId = ""
    private var lastOrderBy = ""
    private var filterType: String? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is DiscussionThreadAdded) {
                    if (lastOrderBy.isNotEmpty()) {
                        updateThread(lastOrderBy)
                    }
                } else if (it is DiscussionThreadDataChanged) {
                    val index = threadsList.indexOfFirst { thread ->
                        thread.id == it.thread.id
                    }
                    if (index >= 0) {
                        threadsList[index] = it.thread
                        _uiState.value = DiscussionThreadsUIState.Threads(threadsList.toList())
                    }
                }
            }
        }
    }

    fun getThreadByType(orderBy: String) {
        _uiState.value = DiscussionThreadsUIState.Loading
        internalLoadThreads(orderBy)
    }

    fun updateThread(orderBy: String) {
        _isUpdating.value = true
        threadsList.clear()
        nextPage = 1
        internalLoadThreads(orderBy)
    }

    fun fetchMore() {
        if (!isLoading && nextPage != -1) {
            isLoading = true
            internalLoadThreads(lastOrderBy)
        }
    }

    private fun internalLoadThreads(orderBy: String) {
        if (lastOrderBy != orderBy) {
            threadsList.clear()
            nextPage = 1
        }
        lastOrderBy = orderBy
        when (threadType) {
            DiscussionTopicsFragment.ALL_POSTS -> {
                getAllThreads(orderBy)
            }

            DiscussionTopicsFragment.FOLLOWING_POSTS -> {
                getFollowingThreads(orderBy)
            }

            DiscussionTopicsFragment.TOPIC -> {
                getThreads(
                    topicId,
                    orderBy
                )
            }
        }
    }

    fun filterThreads(filter: String?) {
        if (filterType != filter || (filter != FilterType.ALL_POSTS.value && filterType.isNullOrEmpty())) {
            threadsList.clear()
            nextPage = 1
        }
        filterType = if (filter == FilterType.ALL_POSTS.value) {
            null
        } else {
            filter
        }
        when (threadType) {
            DiscussionTopicsFragment.ALL_POSTS -> {
                getAllThreads(lastOrderBy)
            }

            DiscussionTopicsFragment.FOLLOWING_POSTS -> {
                getFollowingThreads(lastOrderBy)
            }

            DiscussionTopicsFragment.TOPIC -> {
                getThreads(
                    topicId,
                    lastOrderBy
                )
            }
        }
    }

    private fun getThreads(topicId: String, orderBy: String) {
        viewModelScope.launch {
            try {
                val response =
                    interactor.getThreads(courseId, topicId, orderBy, filterType, nextPage)
                if (response.pagination.next.isNotEmpty()) {
                    _canLoadMore.value = true
                    nextPage++
                } else {
                    _canLoadMore.value = false
                    nextPage = -1
                }
                threadsList.addAll(response.results)
                _uiState.value = DiscussionThreadsUIState.Threads(threadsList.toList())
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
            isLoading = false
        }
    }

    private fun getAllThreads(orderBy: String) {
        viewModelScope.launch {
            try {
                val response = interactor.getAllThreads(courseId, orderBy, filterType, nextPage)
                if (response.pagination.next.isNotEmpty()) {
                    _canLoadMore.value = true
                    nextPage++
                } else {
                    _canLoadMore.value = false
                    nextPage = -1
                }
                threadsList.addAll(response.results)
                _uiState.value = DiscussionThreadsUIState.Threads(threadsList.toList())
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
            isLoading = false
        }
    }

    private fun getFollowingThreads(orderBy: String) {
        viewModelScope.launch {
            try {
                val response =
                    interactor.getFollowingThreads(courseId, true, orderBy, page = nextPage)
                if (response.pagination.next.isNotEmpty()) {
                    _canLoadMore.value = true
                    nextPage++
                } else {
                    _canLoadMore.value = false
                    nextPage = -1
                }
                threadsList.addAll(response.results)
                _uiState.value = DiscussionThreadsUIState.Threads(threadsList.toList())
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
            isLoading = false
        }
    }
}