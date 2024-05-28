package org.openedx.profile.presentation.calendar

import org.openedx.core.BaseViewModel
import org.openedx.core.system.CalendarManager

class NewCalendarDialogViewModel(
    private val calendarManager: CalendarManager
) : BaseViewModel() {

    fun syncCalendar(calendarName: String, calendarColor: CalendarColor) {
        val calendarId = calendarManager.createOrUpdateCalendar(calendarName, calendarColor.color)
//        if (calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST) {
//
//        }
//        val courseDateBlock =  CourseDateBlock(
//            title = "Homework 1: ABCD",
//            description = "After this date, course content will be archived",
//            date = Date(),
//        )
//        calendarManager.addEventsIntoCalendar(
//            calendarId = calendarId,
//            courseId = "",
//            courseName = "",
//            courseDateBlock = courseDateBlock
//        )
    }
}
