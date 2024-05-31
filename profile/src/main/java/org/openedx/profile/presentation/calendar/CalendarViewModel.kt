package org.openedx.profile.presentation.calendar

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.CalendarManager

class CalendarViewModel(
    private val calendarManager: CalendarManager,
    private val corePreferences: CorePreferences,
    private val calendarNotifier: CalendarNotifier
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUIState(
            isCalendarExist = corePreferences.calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST
        )
    )
    val uiState: StateFlow<CalendarUIState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            calendarNotifier.notifier.collect { calendarEvent ->
                when (calendarEvent) {
                    CalendarCreated -> {
                        calendarManager.startSyncCalendarService()
                        _uiState.update { it.copy(isCalendarExist = true) }
                    }
                }
            }
        }
    }

    fun setUpCalendarSync(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        permissionLauncher.launch(calendarManager.permissions)
    }
}
