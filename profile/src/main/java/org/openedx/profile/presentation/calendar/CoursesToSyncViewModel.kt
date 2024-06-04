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

class CoursesToSyncViewModel(
    private val calendarInteractor: CalendarInteractor,
    private val calendarPreferences: CalendarPreferences
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        CoursesToSyncUIState(
            enrollmentsStatus = emptyList(),
            isHideInactiveCourses = calendarPreferences.isHideInactiveCourses
        )
    )
    val uiState: StateFlow<CoursesToSyncUIState>
        get() = _uiState.asStateFlow()

    init {
        getEnrollmentsStatus()
    }

    fun setHideInactiveCoursesEnabled(isEnabled: Boolean) {
        calendarPreferences.isHideInactiveCourses = isEnabled
        _uiState.update { it.copy(isHideInactiveCourses = isEnabled) }
    }

    private fun getEnrollmentsStatus() {
        viewModelScope.launch {
            val enrollmentsStatus = calendarInteractor.getEnrollmentsStatus()
            _uiState.update { it.copy(enrollmentsStatus = enrollmentsStatus) }
        }
    }
}
