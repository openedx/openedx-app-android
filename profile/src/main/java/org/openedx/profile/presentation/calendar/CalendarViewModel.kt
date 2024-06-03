package org.openedx.profile.presentation.calendar

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.CalendarSyncServiceInitiator
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.profile.system.notifier.CalendarNotifier

class CalendarViewModel(
    private val calendarSyncServiceInitiator: CalendarSyncServiceInitiator,
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier,
    private val networkConnection: NetworkConnection
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUIState(
            isCalendarExist = calendarPreferences.calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST,
            calendarData = null,
            calendarSyncState = if (networkConnection.isOnline()) CalendarSyncState.SYNCHRONIZATION else CalendarSyncState.OFFLINE,
            isCalendarSyncEnabled = calendarPreferences.isCalendarSyncEnabled,
            isRelativeDateEnabled = calendarPreferences.isRelativeDateEnabled
        )
    )
    val uiState: StateFlow<CalendarUIState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            calendarNotifier.notifier.collect { calendarEvent ->
                when (calendarEvent) {
                    CalendarCreated -> {
                        calendarSyncServiceInitiator.startSyncCalendarService()
                        _uiState.update { it.copy(isCalendarExist = true) }
                        getCalendarData()
                    }
                }
            }
        }

        getCalendarData()
    }

    fun setUpCalendarSync(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        permissionLauncher.launch(calendarManager.permissions)
    }

    fun setCalendarSyncEnabled(isEnabled: Boolean) {
        calendarPreferences.isCalendarSyncEnabled = isEnabled
        _uiState.update { it.copy(isCalendarSyncEnabled = isEnabled) }
        calendarSyncServiceInitiator.startSyncCalendarService()
    }

    fun setRelativeDateEnabled(isEnabled: Boolean) {
        calendarPreferences.isRelativeDateEnabled = isEnabled
        _uiState.update { it.copy(isRelativeDateEnabled = isEnabled) }
    }

    private fun getCalendarData() {
        val calendarData = calendarManager.getCalendarData(calendarId = calendarPreferences.calendarId)
        _uiState.update { it.copy(calendarData = calendarData) }
    }
}
