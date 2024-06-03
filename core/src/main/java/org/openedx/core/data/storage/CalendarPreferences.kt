package org.openedx.core.data.storage

interface CalendarPreferences {
    var calendarId: Long
    var lastCalendarSync: Long
    var isCalendarSyncEnabled: Boolean
    var isRelativeDateEnabled: Boolean
}
