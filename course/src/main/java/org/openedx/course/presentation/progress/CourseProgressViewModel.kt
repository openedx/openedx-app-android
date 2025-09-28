package org.openedx.course.presentation.progress

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.RefreshProgress
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage

class CourseProgressViewModel(
    val courseId: String,
    private val interactor: CourseInteractor,
    private val courseNotifier: CourseNotifier,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<CourseProgressUIState>(CourseProgressUIState.Loading)
    val uiState: StateFlow<CourseProgressUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private var progressJob: Job? = null

    init {
        loadCourseProgress(false)
        collectCourseNotifier()
    }

    fun loadCourseProgress(isRefresh: Boolean) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            if (!isRefresh) {
                _uiState.value = CourseProgressUIState.Loading
            }
            interactor.getCourseProgress(courseId, isRefresh)
                .catch { e ->
                    if (_uiState.value !is CourseProgressUIState.Data) {
                        _uiState.value = CourseProgressUIState.Error
                    }
                    courseNotifier.send(CourseLoading(false))
                }
                .collectLatest { progress ->
                    _uiState.value = CourseProgressUIState.Data(progress)
                    courseNotifier.send(CourseLoading(false))
                }
        }
    }

    private fun collectCourseNotifier() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is RefreshProgress, is CourseStructureUpdated -> loadCourseProgress(true)
                }
            }
        }
    }
}
