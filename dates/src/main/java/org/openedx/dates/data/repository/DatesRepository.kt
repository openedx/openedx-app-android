package org.openedx.dates.data.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.room.CourseDatesResponseEntity
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.CourseDatesResponse
import org.openedx.dates.data.storage.DatesDao

class DatesRepository(
    private val api: CourseApi,
    private val dao: DatesDao,
    private val preferencesManager: CorePreferences
) {
    suspend fun getUserDates(page: Int): CourseDatesResponse {
        val username = preferencesManager.user?.username ?: ""
        val response = api.getUserDates(username, page)
        if (page == 1) {
            dao.clearCachedData()
        }
        dao.insertCourseDateResponses(CourseDatesResponseEntity.createFrom(response))
        return response.mapToDomain()
    }

    suspend fun getUserDatesFromCache(): List<CourseDate> {
        return dao.getCourseDateResponses()
            .map { it.mapToDomain() }
            .map { it.results }
            .flatten()
            .sortedBy { it.dueDate }
    }

    suspend fun preloadFirstPageCachedDates(): CourseDatesResponse? {
        return dao.getCourseDateResponses()
            .find { it.previous == null }
            ?.mapToDomain()
    }

    suspend fun shiftDueDate() = api.shiftDueDate()
}
