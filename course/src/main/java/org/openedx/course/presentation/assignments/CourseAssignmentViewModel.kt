package org.openedx.course.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.Progress
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseRouter

class CourseAssignmentViewModel(
    val courseId: String,
    val courseRouter: CourseRouter,
    private val courseName: String,
    private val interactor: CourseInteractor,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<CourseAssignmentUIState>(CourseAssignmentUIState.Loading)
    val uiState: StateFlow<CourseAssignmentUIState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            interactor.getCourseStructureFlow(courseId)
                .catch {
                    _uiState.value = CourseAssignmentUIState.Empty
                }
                .collect { courseStructure ->
                    if (courseStructure != null) {
                        updateAssignments(courseStructure)
                    } else {
                        _uiState.value = CourseAssignmentUIState.Empty
                    }
                }
        }
    }

    private fun updateAssignments(courseStructure: CourseStructure) {
        val assignments = courseStructure.blockData
            .filter { !it.assignmentProgress?.assignmentType.isNullOrEmpty() }
        if (assignments.isEmpty()) {
            _uiState.value = CourseAssignmentUIState.Empty
        } else {
            val grouped = assignments.groupBy { it.assignmentProgress?.assignmentType ?: "" }
            val completed = assignments.count { it.isCompleted() }
            val total = assignments.size
            val progress = Progress(completed, total)
            _uiState.value = CourseAssignmentUIState.CourseData(
                groupedAssignments = grouped,
                progress = progress
            )
        }
    }
}
