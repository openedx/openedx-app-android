package org.openedx.core.domain.interactor

import org.openedx.core.data.storage.CourseCalendarEventEntity
import org.openedx.core.repository.CalendarRepository

class CalendarInteractor(
    private val repository: CalendarRepository
) {

    suspend fun getEnrollmentsStatus() = repository.getEnrollmentsStatus()

    suspend fun getCourseDates(courseId: String) = repository.getCourseDates(courseId)

    suspend fun insertCourseCalendarEntity(vararg courseCalendarEntity: CourseCalendarEventEntity) {
        repository.insertCourseCalendarEntity(*courseCalendarEntity)
    }

    suspend fun readAllData(): List<CourseCalendarEventEntity> {
        return repository.readAllData()
    }
}
