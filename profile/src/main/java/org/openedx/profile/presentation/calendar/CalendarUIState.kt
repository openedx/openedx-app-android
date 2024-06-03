package org.openedx.profile.presentation.calendar

import org.openedx.core.domain.model.CalendarData

data class CalendarUIState(
    val isCalendarExist: Boolean,
    val calendarData: CalendarData? = null,
    val calendarSyncState: CalendarSyncState,
    val isCalendarSyncEnabled: Boolean,
    val isRelativeDateEnabled: Boolean,
)