package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class CoursesToSyncViewModel(
    private val calendarInteractor: CalendarInteractor,
    private val calendarPreferences: CalendarPreferences,
    private val calendarSyncScheduler: CalendarSyncScheduler,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        CoursesToSyncUIState(
            enrollmentsStatus = emptyList(),
            coursesCalendarState = emptyList(),
            isHideInactiveCourses = calendarPreferences.isHideInactiveCourses,
            isLoading = true
        )
    )

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

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
            try {
                val coursesCalendarState = calendarInteractor.getAllCourseCalendarStateFromCache()
                _uiState.update { it.copy(coursesCalendarState = coursesCalendarState) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiMessage.emit(
                    UIMessage.SnackBarMessage(
                        resourceManager.getString(R.string.core_error_unknown_error)
                    )
                )
            }
        }
    }

    private fun getEnrollmentsStatus() {
        viewModelScope.launch {
            try {
                val enrollmentsStatus = calendarInteractor.getEnrollmentsStatus()
                _uiState.update { it.copy(enrollmentsStatus = enrollmentsStatus) }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(
                                R.string.core_error_no_connection
                            )
                        )
                    )
                } else {
                    _uiMessage.emit(
                        UIMessage.SnackBarMessage(
                            resourceManager.getString(R.string.core_error_unknown_error)
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
