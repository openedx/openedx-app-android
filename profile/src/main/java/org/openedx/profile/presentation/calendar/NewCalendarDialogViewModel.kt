package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.CalendarManager

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager,
    private val corePreferences: CorePreferences,
    private val calendarNotifier: CalendarNotifier
) : BaseViewModel() {

    fun createCalendar(calendarName: String, calendarColor: CalendarColor) {
        val calendarId = calendarManager.createOrUpdateCalendar(calendarName, calendarColor.color)
        if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
            corePreferences.calendarId = calendarId
            viewModelScope.launch {
                calendarNotifier.send(CalendarCreated)
            }
        }
    }
}
