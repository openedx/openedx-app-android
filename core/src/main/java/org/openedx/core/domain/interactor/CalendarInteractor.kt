package org.openedx.core.domain.interactor

import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.domain.model.CourseCalendarEvent
import org.openedx.core.domain.model.CourseCalendarState
import org.openedx.core.repository.CalendarRepository

class CalendarInteractor(
    private val repository: CalendarRepository
) {

    suspend fun getEnrollmentsStatus() = repository.getEnrollmentsStatus()

    suspend fun getCourseDates(courseId: String) = repository.getCourseDates(courseId)

    suspend fun insertCourseCalendarEntityToCache(vararg courseCalendarEntity: CourseCalendarEventEntity) {
        repository.insertCourseCalendarEntityToCache(*courseCalendarEntity)
    }

    suspend fun getCourseCalendarEventsByIdFromCache(courseId: String): List<CourseCalendarEvent> {
        return repository.getCourseCalendarEventsByIdFromCache(courseId)
    }

    suspend fun deleteCourseCalendarEntitiesByIdFromCache(courseId: String) {
        repository.deleteCourseCalendarEntitiesByIdFromCache(courseId)
    }

    suspend fun insertCourseCalendarStateEntityToCache(vararg courseCalendarStateEntity: CourseCalendarStateEntity) {
        repository.insertCourseCalendarStateEntityToCache(*courseCalendarStateEntity)
    }

    suspend fun getCourseCalendarStateByIdFromCache(courseId: String): CourseCalendarState? {
        return repository.getCourseCalendarStateByIdFromCache(courseId)
    }

    suspend fun getAllCourseCalendarStateFromCache(): List<CourseCalendarState> {
        return repository.getAllCourseCalendarStateFromCache()
    }

    suspend fun clearCalendarCachedData() {
        repository.clearCalendarCachedData()
    }

    suspend fun resetChecksums() {
        repository.resetChecksums()
    }

    suspend fun updateCourseCalendarStateByIdInCache(
        courseId: String,
        checksum: Int? = null,
        isCourseSyncEnabled: Boolean? = null
    ) {
        repository.updateCourseCalendarStateByIdInCache(courseId, checksum, isCourseSyncEnabled)
    }

    suspend fun deleteCourseCalendarStateByIdFromCache(courseId: String) {
        repository.deleteCourseCalendarStateByIdFromCache(courseId)
    }
}
