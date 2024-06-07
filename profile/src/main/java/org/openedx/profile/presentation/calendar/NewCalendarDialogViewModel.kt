package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.notifier.calendar.CalendarCreated
import org.openedx.core.system.notifier.calendar.CalendarNotifier

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier,
    private val calendarInteractor: CalendarInteractor,
) : BaseViewModel() {

    fun createCalendar(
        calendarTitle: String,
        calendarColor: CalendarColor,
    ): Boolean {
        viewModelScope.launch {
            calendarInteractor.resetChecksums()
        }
        val calendarId = calendarManager.createOrUpdateCalendar(
            calendarTitle = calendarTitle,
            calendarColor = calendarColor.color
        )
        return if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
            calendarPreferences.calendarId = calendarId
            calendarPreferences.calendarUser = calendarManager.accountName
            viewModelScope.launch {
                calendarNotifier.send(CalendarCreated)
            }
            true
        } else {
            false
        }
    }
}
