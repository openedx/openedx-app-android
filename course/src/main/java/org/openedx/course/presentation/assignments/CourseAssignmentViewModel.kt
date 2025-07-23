package org.openedx.course.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.Progress
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.outline.CourseContentAllUIState

class CourseAssignmentViewModel(
    val courseId: String,
    val courseRouter: CourseRouter,
    private val courseName: String,
    private val interactor: CourseInteractor,
    private val courseNotifier: CourseNotifier,
    private val analytics: CourseAnalytics,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<CourseAssignmentUIState>(CourseAssignmentUIState.Loading)
    val uiState: StateFlow<CourseAssignmentUIState> = _uiState.asStateFlow()

    init {
        collectCourseNotifier()
        collectData()
    }

    private fun collectData() {
        viewModelScope.launch {
            val courseProgressFlow = interactor.getCourseProgress(courseId, false)
            val courseStructureFlow = interactor.getCourseStructureFlow(courseId)

            combine(
                courseProgressFlow,
                courseStructureFlow
            ) { courseProgress, courseStructure ->
                courseProgress to courseStructure
            }.catch {
                if (_uiState.value !is CourseAssignmentUIState.CourseData) {
                    _uiState.value = CourseAssignmentUIState.Empty
                }
            }.collect { (courseProgress, courseStructure) ->
                if (courseStructure != null) {
                    updateAssignments(courseStructure, courseProgress)
                } else {
                    _uiState.value = CourseAssignmentUIState.Empty
                }
            }
        }
    }

    private fun updateAssignments(
        courseStructure: CourseStructure,
        courseProgress: CourseProgress
    ) {
        val assignments = courseStructure.blockData
            .filter { !it.assignmentProgress?.assignmentType.isNullOrEmpty() }
        if (assignments.isEmpty()) {
            _uiState.value = CourseAssignmentUIState.Empty
        } else {
            val grouped = assignments
                .filter { assignments ->
                    courseProgress.gradingPolicy?.assignmentPolicies?.map { it.type }
                        ?.contains(assignments.assignmentProgress?.assignmentType) == true
                }
                .groupBy { it.assignmentProgress?.assignmentType ?: "" }
            val completed = assignments.count { it.isCompleted() }
            val total = assignments.size
            val progress = Progress(completed, total)
            _uiState.value = CourseAssignmentUIState.CourseData(
                groupedAssignments = grouped,
                courseProgress = courseProgress,
                progress = progress
            )
        }
    }

    private fun collectCourseNotifier() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseStructureUpdated -> collectData()
                }
            }
        }
    }

    fun navigateToSequentialEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseContentAllUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.ASSIGNMENT_CLICKED.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.ASSIGNMENT_CLICKED.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, courseName)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                }
            )
        }
    }
}
