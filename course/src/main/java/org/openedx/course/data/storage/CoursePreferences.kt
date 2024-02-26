package org.openedx.course.data.storage

interface CoursePreferences {
    fun setCalendarSyncEventsDialogShown(courseName: String)
    fun isCalendarSyncEventsDialogShown(courseName: String): Boolean
}
