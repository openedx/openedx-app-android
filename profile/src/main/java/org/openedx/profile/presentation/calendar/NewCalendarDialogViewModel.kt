package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.R
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.domain.model.CalendarType
import org.openedx.core.domain.model.UserCalendar
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.calendar.CalendarCreated
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarNotifier: CalendarNotifier,
    private val calendarInteractor: CalendarInteractor,
    private val networkConnection: NetworkConnection,
    private val resourceManager: ResourceManager,
) : BaseViewModel(resourceManager) {
    private val _isSuccess = MutableSharedFlow<Boolean>()
    val isSuccess: SharedFlow<Boolean>
        get() = _isSuccess.asSharedFlow()

    private val _googleCalendars = MutableStateFlow<List<UserCalendar>>(emptyList())
    val googleCalendars: StateFlow<List<UserCalendar>>
        get() = _googleCalendars.asStateFlow()

    private val _showLocalCalendarSection =
        MutableStateFlow(calendarManager.hasAlternativeCalendarApp())
    val showLocalCalendarSection: StateFlow<Boolean>
        get() = _showLocalCalendarSection.asStateFlow()

    init {
        loadGoogleCalendars()
    }

    private fun loadGoogleCalendars() {
        viewModelScope.launch {
            val calendars = withContext(Dispatchers.IO) {
                calendarManager.getGoogleCalendars()
            }
            _googleCalendars.emit(calendars)
        }
    }

    fun createCalendar(
        calendarTitle: String,
        calendarColor: CalendarColor,
    ) {
        viewModelScope.launch {
            if (networkConnection.isOnline()) {
                calendarInteractor.resetChecksums()
                val currentCalendarType = calendarPreferences.calendarType
                val calendarId = calendarManager.createOrUpdateCalendar(
                    calendarId = calendarPreferences.calendarId.takeIf {
                        currentCalendarType == CalendarType.LOCAL
                    } ?: CalendarManager.CALENDAR_DOES_NOT_EXIST,
                    calendarTitle = calendarTitle,
                    calendarColor = calendarColor.color,
                    calendarType = CalendarType.LOCAL
                )
                if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
                    calendarPreferences.calendarId = calendarId
                    calendarPreferences.calendarUser = calendarManager.accountName
                    calendarPreferences.calendarType = CalendarType.LOCAL
                    viewModelScope.launch {
                        calendarNotifier.send(CalendarCreated)
                    }
                    _isSuccess.emit(true)
                } else {
                    handleErrorUiMessage(
                        throwable = null,
                    )
                }
            } else {
                handleErrorUiMessage(
                    throwable = UnknownHostException(),
                )
            }
        }
    }

    fun syncWithGoogleCalendar(calendarId: Long) {
        viewModelScope.launch {
            if (!networkConnection.isOnline()) {
                sendMessage(
                    UIMessage.SnackBarMessage(
                        resourceManager.getString(R.string.core_error_no_connection)
                    )
                )
                return@launch
            }

            if (!calendarManager.isCalendarExist(calendarId)) {
                sendMessage(
                    UIMessage.SnackBarMessage(
                        resourceManager.getString(
                            R.string.core_error_unknown_error
                        )
                    )
                )
                return@launch
            }

            calendarInteractor.resetChecksums()
            if (calendarPreferences.calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST &&
                calendarPreferences.calendarId != calendarId &&
                calendarPreferences.calendarType == CalendarType.LOCAL
            ) {
                calendarManager.deleteCalendar(calendarPreferences.calendarId)
            }

            calendarPreferences.calendarId = calendarId
            calendarPreferences.calendarUser = calendarManager.accountName
            calendarPreferences.calendarType = CalendarType.GOOGLE
            calendarNotifier.send(CalendarCreated)
            _isSuccess.emit(true)
        }
    }
}
