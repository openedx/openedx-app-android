package org.openedx.core.data.storage

interface CalendarPreferences {
    var calendarId: Long
    var isCalendarSyncEnabled: Boolean
    var isHideInactiveCourses: Boolean
}
