package org.openedx.course.presentation.dates

import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState

sealed interface DatesUIState {
    data class Dates(
        val courseDatesResult: CourseDatesResult,
        val calendarSyncState: CalendarSyncState
    ) : DatesUIState

    data object Empty : DatesUIState
    data object Loading : DatesUIState
}
