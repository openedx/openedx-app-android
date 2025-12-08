package org.openedx.core.data.storage

import org.openedx.core.domain.model.CalendarType

interface CalendarPreferences {
    var calendarId: Long
    var calendarUser: String
    var calendarType: CalendarType
    var isCalendarSyncEnabled: Boolean
    var isHideInactiveCourses: Boolean

    fun clearCalendarPreferences()
}
