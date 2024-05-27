package org.openedx.profile.presentation.calendar

import org.openedx.core.BaseViewModel
import org.openedx.core.system.CalendarManager

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager
) : BaseViewModel() {

    fun syncCalendar(calendarName: String, calendarColor: CalendarColor) {
        calendarManager.createOrUpdateCalendar(calendarName, calendarColor.color)
    }
}
