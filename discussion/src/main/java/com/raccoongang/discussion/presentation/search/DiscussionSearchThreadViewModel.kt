package com.raccoongang.discussion.presentation.search

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
import com.raccoongang.discussion.system.notifier.DiscussionNotifier
import com.raccoongang.discussion.system.notifier.DiscussionThreadDataChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class DiscussionSearchThreadViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    val courseId: String
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DiscussionSearchThreadUIState>(
        DiscussionSearchThreadUIState.Threads(
            emptyList(), 0
        )
    )
    val uiState: LiveData<DiscussionSearchThreadUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating


    private var nextPage: Int? = 1
    private var currentQuery: String? = null
    private val threadsList = mutableListOf<com.raccoongang.discussion.domain.model.Thread>()
    private var isLoading = false

    private val queryChannel = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 0)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is DiscussionThreadDataChanged) {
                    val index = threadsList.indexOfFirst { thread ->
                        thread.id == it.thread.id
                    }
                    if (index >= 0) {
                        threadsList[index] = it.thread
                        val count =
                            (uiState.value as? DiscussionSearchThreadUIState.Threads)?.count ?: 0
                        _uiState.value =
                            DiscussionSearchThreadUIState.Threads(threadsList.toList(), count)
                    }
                }
            }
        }
    }

    init {
        observeQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            queryChannel
                .asSharedFlow()
                .debounce(400)
                .collect {
                    nextPage = 1
                    currentQuery = it
                    threadsList.clear()
                    _uiState.value = DiscussionSearchThreadUIState.Loading
                    loadThreadsInternal(currentQuery!!, nextPage!!)
                }
        }
    }

    fun searchThreads(query: String) {
        viewModelScope.launch {
            if (query.trim().isNotEmpty()) {
                queryChannel.emit(query.trim())
            } else {
                currentQuery = null
                nextPage = 1
                threadsList.clear()
                _uiState.value = DiscussionSearchThreadUIState.Threads(emptyList(), 0)
            }
        }
    }

    fun updateSearchQuery() {
        currentQuery?.let {
            nextPage = 1
            isLoading = true
            _isUpdating.value = true
            threadsList.clear()
            loadThreadsInternal(it, nextPage!!)
        }
    }

    fun fetchMore() {
        if (!isLoading && nextPage != null) {
            currentQuery?.let {
                loadThreadsInternal(it, nextPage!!)
            }
        }
    }

    private fun loadThreadsInternal(query: String, page: Int) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = interactor.searchThread(courseId, query, page)
                if (response.pagination.next.isNotEmpty() && page < response.pagination.numPages) {
                    _canLoadMore.value = true
                    nextPage = page + 1
                } else {
                    _canLoadMore.value = false
                    nextPage = null
                }
                threadsList.addAll(response.results)
                _uiState.value =
                    DiscussionSearchThreadUIState.Threads(threadsList, response.pagination.count)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                isLoading = false
                _isUpdating.value = false
            }
        }
    }

}