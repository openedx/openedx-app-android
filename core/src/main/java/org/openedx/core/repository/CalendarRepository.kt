package org.openedx.core.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.CourseCalendarEventEntity
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

    suspend fun insertCourseCalendarEntity(vararg courseCalendarEntity: CourseCalendarEventEntity) {
        calendarDao.insertCourseCalendarEntity(*courseCalendarEntity)
    }

    suspend fun readAllData(): List<CourseCalendarEventEntity> {
        return calendarDao.readAllData()
    }
}
