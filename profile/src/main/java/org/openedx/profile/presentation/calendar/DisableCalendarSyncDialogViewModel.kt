package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.core.system.notifier.calendar.CalendarSyncDisabled
import org.openedx.foundation.presentation.BaseViewModel

class DisableCalendarSyncDialogViewModel(
    private val calendarNotifier: CalendarNotifier,
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarInteractor: CalendarInteractor,
) : BaseViewModel() {

    fun disableSyncingClick() {
        viewModelScope.launch {
            calendarInteractor.clearCalendarCachedData()
            calendarManager.deleteCalendar(calendarPreferences.calendarId)
            calendarPreferences.clearCalendarPreferences()
            calendarNotifier.send(CalendarSyncDisabled)
        }
    }
}
