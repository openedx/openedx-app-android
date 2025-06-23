package org.openedx.course.presentation.progress

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
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

    init {
        loadCourseProgress()
        collectCourseNotifier()
    }

    fun loadCourseProgress() {
        viewModelScope.launch {
            _uiState.value = CourseProgressUIState.Loading
            try {
                val progress = interactor.getCourseProgress(courseId)
                _uiState.value = CourseProgressUIState.Data(progress)
            } catch (_: Exception) {
                _uiState.value = CourseProgressUIState.Error
            }
        }
    }


    private fun collectCourseNotifier() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is RefreshProgress, is CourseCompletionSet -> loadCourseProgress()
                }
            }
        }
    }
}
