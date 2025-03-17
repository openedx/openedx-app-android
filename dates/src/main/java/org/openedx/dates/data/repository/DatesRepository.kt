package org.openedx.dates.data.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseDate
import org.openedx.core.domain.model.CourseDatesResponse
import org.openedx.dates.data.storage.CourseDateEntity
import org.openedx.dates.data.storage.DatesDao

class DatesRepository(
    private val api: CourseApi,
    private val dao: DatesDao,
    private val preferencesManager: CorePreferences
) {
    suspend fun getUserDates(page: Int): CourseDatesResponse {
        val username = preferencesManager.user?.username ?: ""
        val response = api.getUserDates(username, page)
        dao.insertCourseDateEntities(response.results.map { CourseDateEntity.createFrom(it) })
        return response.mapToDomain()
    }

    suspend fun getUserDatesFromCache(): List<CourseDate> {
        return dao.getCourseDateEntities().mapNotNull { it.mapToDomain() }
    }
}
