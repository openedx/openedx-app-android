package org.openedx.core.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseCalendarEvent
import org.openedx.core.domain.model.CourseCalendarState
import org.openedx.core.domain.model.EnrollmentStatus
import org.openedx.core.module.db.CalendarDao

class CalendarRepository(
    private val api: CourseApi,
    private val corePreferences: CorePreferences,
    private val calendarDao: CalendarDao
) {

    suspend fun getEnrollmentsStatus(): List<EnrollmentStatus> {
        val response = api.getEnrollmentsStatus(corePreferences.user?.username ?: "")
        return response.map { it.mapToDomain() }
    }

    suspend fun getCourseDates(courseId: String) = api.getCourseDates(courseId)

    suspend fun insertCourseCalendarEntityToCache(vararg courseCalendarEntity: CourseCalendarEventEntity) {
        calendarDao.insertCourseCalendarEntity(*courseCalendarEntity)
    }

    suspend fun getCourseCalendarEventsByIdFromCache(courseId: String): List<CourseCalendarEvent> {
        return calendarDao.readCourseCalendarEventsById(courseId).map { it.mapToDomain() }
    }

    suspend fun deleteCourseCalendarEntitiesByIdFromCache(courseId: String) {
        calendarDao.deleteCourseCalendarEntitiesById(courseId)
    }

    suspend fun insertCourseCalendarStateEntityToCache(vararg courseCalendarStateEntity: CourseCalendarStateEntity) {
        calendarDao.insertCourseCalendarStateEntity(*courseCalendarStateEntity)
    }

    suspend fun getCourseCalendarStateByIdFromCache(courseId: String): CourseCalendarState? {
        return calendarDao.readCourseCalendarStateById(courseId)?.mapToDomain()
    }

    suspend fun getAllCourseCalendarStateFromCache(): List<CourseCalendarState> {
        return calendarDao.readAllCourseCalendarState().map { it.mapToDomain() }
    }

    suspend fun resetChecksums() {
        calendarDao.resetChecksums()
    }

    suspend fun clearCalendarCachedData() {
        calendarDao.clearCachedData()
    }

    suspend fun updateCourseCalendarStateByIdInCache(
        courseId: String,
        checksum: Int? = null,
        isCourseSyncEnabled: Boolean? = null
    ) {
        calendarDao.updateCourseCalendarStateById(courseId, checksum, isCourseSyncEnabled)
    }

    suspend fun deleteCourseCalendarStateByIdFromCache(courseId: String) {
        calendarDao.deleteCourseCalendarStateById(courseId)
    }
}
