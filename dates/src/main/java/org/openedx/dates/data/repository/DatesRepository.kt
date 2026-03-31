package org.openedx.dates.data.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.room.CourseDateEntity
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
        dao.insertCourseDates(response.results.map { CourseDateEntity.createFrom(it) })
        return response.mapToDomain()
    }

    suspend fun getUserDatesFromCache(): List<CourseDate> {
        return dao.getCourseDates().mapNotNull { it.mapToDomain() }
    }

    suspend fun preloadFirstPageCachedDates(): List<CourseDate> {
        return dao.getCourseDates(PAGE_SIZE).mapNotNull { it.mapToDomain() }
    }

    suspend fun shiftAllDueDates() = api.shiftAllDueDates()

    companion object {
        private const val PAGE_SIZE = 20
    }
}
