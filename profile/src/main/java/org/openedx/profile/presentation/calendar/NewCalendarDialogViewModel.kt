package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.calendar.CalendarCreated
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier,
    private val calendarInteractor: CalendarInteractor,
    private val networkConnection: NetworkConnection,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String>
        get() = _uiMessage.asSharedFlow()

    private val _isSuccess = MutableSharedFlow<Boolean>()
    val isSuccess: SharedFlow<Boolean>
        get() = _isSuccess.asSharedFlow()

    fun createCalendar(
        calendarTitle: String,
        calendarColor: CalendarColor,
    ) {
        viewModelScope.launch {
            if (networkConnection.isOnline()) {
                calendarInteractor.resetChecksums()
                val calendarId = calendarManager.createOrUpdateCalendar(
                    calendarId = calendarPreferences.calendarId,
                    calendarTitle = calendarTitle,
                    calendarColor = calendarColor.color
                )
                if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
                    calendarPreferences.calendarId = calendarId
                    calendarPreferences.calendarUser = calendarManager.accountName
                    viewModelScope.launch {
                        calendarNotifier.send(CalendarCreated)
                    }
                    _isSuccess.emit(true)
                } else {
                    _uiMessage.emit(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } else {
                _uiMessage.emit(resourceManager.getString(R.string.core_error_no_connection))
            }
        }
    }
}
