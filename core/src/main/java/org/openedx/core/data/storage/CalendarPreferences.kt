package org.openedx.core.data.storage

interface CalendarPreferences {
    var calendarId: Long
    var calendarUser: String
    var isCalendarSyncEnabled: Boolean
    var isHideInactiveCourses: Boolean

    fun clearCalendarPreferences()
}
