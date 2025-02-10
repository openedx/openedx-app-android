package org.openedx.discussion.presentation.search

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadDataChanged
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class DiscussionSearchThreadViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    val courseId: String
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DiscussionSearchThreadUIState>(
        DiscussionSearchThreadUIState.Threads(
            emptyList(),
            0
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
    private val threadsList = mutableListOf<org.openedx.discussion.domain.model.Thread>()
    private var isLoading = false
    private var loadNextJob: Job? = null

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
                .debounce(SEARCH_DEBOUNCE)
                .collect { query ->
                    nextPage = 1
                    threadsList.clear()
                    if (query.isNotEmpty()) {
                        currentQuery = query
                        _uiState.value = DiscussionSearchThreadUIState.Loading
                        loadThreadsInternal(currentQuery!!, nextPage!!)
                    } else {
                        loadNextJob?.cancel()
                        currentQuery = null
                        _uiState.value = DiscussionSearchThreadUIState.Threads(emptyList(), 0)
                        _canLoadMore.value = false
                    }
                }
        }
    }

    fun searchThreads(query: String) {
        viewModelScope.launch {
            queryChannel.emit(query.trim())
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
        loadNextJob?.cancel()
        loadNextJob = flow {
            emit(interactor.searchThread(courseId, query, page))
        }
            .cancellable()
            .onEach { response ->
                isLoading = true
                if (response.pagination.next.isNotEmpty() && page < response.pagination.numPages) {
                    _canLoadMore.value = true
                    nextPage = page + 1
                } else {
                    _canLoadMore.value = false
                    nextPage = null
                }
                threadsList.addAll(response.results)
                _uiState.value =
                    DiscussionSearchThreadUIState.Threads(
                        threadsList, response.pagination.count
                    )
                isLoading = false
                _isUpdating.value = false
            }.catch { e ->
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
                isLoading = false
                _isUpdating.value = false
            }
            .launchIn(viewModelScope)
    }

    companion object {
        private const val SEARCH_DEBOUNCE = 400L
    }
}
