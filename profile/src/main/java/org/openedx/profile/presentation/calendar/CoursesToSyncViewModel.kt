package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.worker.CalendarSyncScheduler

class CoursesToSyncViewModel(
    private val calendarInteractor: CalendarInteractor,
    private val calendarPreferences: CalendarPreferences,
    private val calendarSyncScheduler: CalendarSyncScheduler,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        CoursesToSyncUIState(
            enrollmentsStatus = emptyList(),
            coursesCalendarState = emptyList(),
            isHideInactiveCourses = calendarPreferences.isHideInactiveCourses,
            isLoading = true
        )
    )
    val uiState: StateFlow<CoursesToSyncUIState>
        get() = _uiState.asStateFlow()

    init {
        getEnrollmentsStatus()
        getCourseCalendarState()
    }

    fun setHideInactiveCoursesEnabled(isEnabled: Boolean) {
        calendarPreferences.isHideInactiveCourses = isEnabled
        _uiState.update { it.copy(isHideInactiveCourses = isEnabled) }
    }

    fun setCourseSyncEnabled(isEnabled: Boolean, courseId: String) {
        viewModelScope.launch {
            calendarInteractor.updateCourseCalendarStateByIdInCache(
                courseId = courseId,
                isCourseSyncEnabled = isEnabled
            )
            getCourseCalendarState()
            calendarSyncScheduler.requestImmediateSync(courseId)
        }
    }

    private fun getCourseCalendarState() {
        viewModelScope.launch {
            val coursesCalendarState = calendarInteractor.getAllCourseCalendarStateFromCache()
            _uiState.update { it.copy(coursesCalendarState = coursesCalendarState) }
        }
    }

    private fun getEnrollmentsStatus() {
        viewModelScope.launch {
            val enrollmentsStatus = calendarInteractor.getEnrollmentsStatus()
            _uiState.update { it.copy(enrollmentsStatus = enrollmentsStatus, isLoading = false) }
        }
    }
}
