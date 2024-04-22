package org.openedx.core.system.notifier

import org.openedx.core.domain.model.CourseDateBlock

sealed class CalendarSyncEvent : CourseEvent {
    class CreateCalendarSyncEvent(
        val courseDates: List<CourseDateBlock>,
        val dialogType: String,
        val checkOutOfSync: Boolean,
    ) : CalendarSyncEvent()

    class CheckCalendarSyncEvent(val isSynced: Boolean) : CalendarSyncEvent()
}
