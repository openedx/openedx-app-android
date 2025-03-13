package org.openedx.dates.data.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseDatesResponse

class DatesRepository(
    private val api: CourseApi,
    private val preferencesManager: CorePreferences
) {
    suspend fun getUserDates(): CourseDatesResponse {
        val username = preferencesManager.user?.username ?: ""
        return api.getUserDates(username).mapToDomain()
    }
}
