package com.raccoongang.discovery.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.discovery.domain.interactor.DiscoveryInteractor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class CourseSearchViewModel(
    private val interactor: DiscoveryInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val _uiState =
        MutableLiveData<CourseSearchUIState>(CourseSearchUIState.Courses(emptyList(), 0))
    val uiState: LiveData<CourseSearchUIState>
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
    private val coursesList = mutableListOf<Course>()
    private var isLoading = false

    private val queryChannel = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 0)

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
                    coursesList.clear()
                    _uiState.value = CourseSearchUIState.Loading
                    loadCoursesInternal(currentQuery!!, nextPage!!)
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.trim().isNotEmpty()) {
                queryChannel.emit(query.trim())
            } else {
                currentQuery = null
                nextPage = 1
                coursesList.clear()
                _uiState.value = CourseSearchUIState.Courses(emptyList(), 0)
            }
        }
    }

    fun fetchMore() {
        if (!isLoading && nextPage != null) {
            currentQuery?.let {
                loadCoursesInternal(it, nextPage!!)
            }
        }
    }

    fun updateSearchQuery() {
        currentQuery?.let {
            nextPage = 1
            isLoading = true
            _isUpdating.value = true
            coursesList.clear()
            loadCoursesInternal(it, nextPage!!)
        }
    }

    private fun loadCoursesInternal(query: String, page: Int) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = interactor.getCoursesListByQuery(query, page)
                if (response.pagination.next.isNotEmpty() && page < response.pagination.numPages) {
                    _canLoadMore.value = true
                    nextPage = page + 1
                } else {
                    _canLoadMore.value = false
                    nextPage = null
                }
                coursesList.addAll(response.results)
                _uiState.value = CourseSearchUIState.Courses(coursesList, response.pagination.count)
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