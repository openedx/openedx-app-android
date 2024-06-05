package org.openedx.core.domain.interactor

import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.repository.CalendarRepository

class CalendarInteractor(
    private val repository: CalendarRepository
) {

    suspend fun getEnrollmentsStatus() = repository.getEnrollmentsStatus()

    suspend fun getCourseDates(courseId: String) = repository.getCourseDates(courseId)

    suspend fun insertCourseCalendarEntity(vararg courseCalendarEntity: CourseCalendarEventEntity) {
        repository.insertCourseCalendarEntity(*courseCalendarEntity)
    }

    suspend fun getCourseCalendarEventsById(courseId: String): List<CourseCalendarEventEntity> {
        return repository.getCourseCalendarEventsById(courseId)
    }

    suspend fun deleteCourseCalendarEntitiesById(courseId: String) {
        repository.deleteCourseCalendarEntitiesById(courseId)
    }

    suspend fun insertCourseCalendarStateEntity(vararg courseCalendarStateEntity: CourseCalendarStateEntity) {
        repository.insertCourseCalendarStateEntity(*courseCalendarStateEntity)
    }

    suspend fun getCourseCalendarStateById(courseId: String): CourseCalendarStateEntity? {
        return repository.getCourseCalendarStateById(courseId)
    }

    suspend fun getAllCourseCalendarState(): List<CourseCalendarStateEntity> {
        return repository.getAllCourseCalendarState()
    }

    suspend fun updateCourseCalendarStateById(
        courseId: String,
        checksum: Int? = null,
        isCourseSyncEnabled: Boolean? = null
    ) {
        return repository.updateCourseCalendarStateById(courseId, checksum, isCourseSyncEnabled)
    }
}
