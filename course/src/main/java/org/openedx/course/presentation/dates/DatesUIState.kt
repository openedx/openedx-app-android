package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState

sealed class DatesUIState {
    data class Dates(
        val courseDatesResult: CourseDatesResult,
        val calendarSyncState: CalendarSyncState,
    ) : DatesUIState()
    data object Error : DatesUIState()
    data object Loading : DatesUIState()
}
