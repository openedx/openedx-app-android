package org.openedx.profile.presentation.calendar

import org.openedx.core.domain.model.CalendarData
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState

data class CalendarUIState(
    val isCalendarExist: Boolean,
    val calendarData: CalendarData? = null,
    val calendarSyncState: CalendarSyncState,
    val isCalendarSyncEnabled: Boolean,
    val coursesSynced: Int?,
    val isRelativeDateEnabled: Boolean,
)
