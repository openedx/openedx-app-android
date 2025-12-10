package org.openedx.profile.presentation.calendar

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.domain.model.CalendarType
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.core.system.notifier.calendar.CalendarSyncDisabled
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager

class DisableCalendarSyncDialogViewModel(
    private val calendarNotifier: CalendarNotifier,
    private val calendarManager: CalendarManager,
    private val calendarPreferences: CalendarPreferences,
    private val calendarInteractor: CalendarInteractor,
    private val resourceManager: ResourceManager,
) : BaseViewModel(resourceManager) {

    private val _deletionState = MutableStateFlow<DeletionState?>(null)
    val deletionState: StateFlow<DeletionState?> = _deletionState.asStateFlow()

    fun disableSyncingClick() {
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    _deletionState.value = DeletionState.DELETING
                    val allEvents = calendarInteractor.getAllCourseCalendarEventsFromCache()
                    val eventIds = allEvents.map { it.eventId }
                    calendarManager.deleteEvents(eventIds)
                    _deletionState.value = DeletionState.DELETED
                    calendarInteractor.clearCalendarCachedData()
                    val calendarId = calendarPreferences.calendarId
                    if (calendarPreferences.calendarType == CalendarType.LOCAL) {
                        calendarManager.deleteCalendar(calendarId)
                    }
                    calendarPreferences.clearCalendarPreferences()
                    calendarNotifier.send(CalendarSyncDisabled)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _deletionState.value = null
            }
        }
    }
}

enum class DeletionState {
    DELETING, DELETED
}
