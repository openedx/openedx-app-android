package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.profile.system.notifier.CalendarNotifier
import org.openedx.profile.system.notifier.CalendarSyncDisabled

class DisableCalendarSyncDialogViewModel(
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier
) : BaseViewModel() {

    fun disableSyncingClick() {
        calendarPreferences.isCalendarSyncEnabled = false
        viewModelScope.launch {
            calendarNotifier.send(CalendarSyncDisabled)
        }
    }
}
