package org.openedx.course.presentation.progress

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.openedx.core.system.notifier.CourseLoading
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseProgressLoaded
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

    init {
        collectData(false)
        collectCourseNotifier()
    }

    private fun collectData(isRefresh: Boolean) {
        viewModelScope.launch {
            val courseProgressFlow = interactor.getCourseProgress(courseId, isRefresh, false)
            val courseStructureFlow = interactor.getCourseStructureFlow(courseId)

            combine(
                courseProgressFlow,
                courseStructureFlow
            ) { courseProgress, courseStructure ->
                courseProgress to courseStructure
            }.catch { e ->
                if (_uiState.value !is CourseProgressUIState.Data) {
                    _uiState.value = CourseProgressUIState.Error
                }
                courseNotifier.send(CourseLoading(false))
            }.collect { (courseProgress, courseStructure) ->
                _uiState.value = CourseProgressUIState.Data(
                    courseProgress,
                    courseStructure
                )
                courseNotifier.send(CourseLoading(false))
                courseNotifier.send(CourseProgressLoaded)
            }
        }
    }

    private fun collectCourseNotifier() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is RefreshProgress, is CourseStructureUpdated -> collectData(true)
                }
            }
        }
    }
}
