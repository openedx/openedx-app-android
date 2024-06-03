package org.openedx.profile.presentation.calendar

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.extension.toastMessage
import org.openedx.core.system.CalendarManager
import org.openedx.profile.system.notifier.CalendarNotifier
import org.openedx.core.R as coreR

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier
) : BaseViewModel() {

    fun createCalendar(
        context: Context,
        calendarTitle: String,
        calendarColor: CalendarColor,
    ) {
        val calendarId = calendarManager.createOrUpdateCalendar(
            calendarTitle = calendarTitle,
            calendarColor = calendarColor.color
        )
        if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
            calendarPreferences.calendarId = calendarId
            viewModelScope.launch {
                calendarNotifier.send(CalendarCreated)
            }
        } else {
            context.toastMessage(context.getString(coreR.string.core_error_unknown_error))
        }
    }
}
