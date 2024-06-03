package org.openedx.core.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.EnrollmentStatus

class CalendarRepository(
    private val api: CourseApi,
    private val corePreferences: CorePreferences
) {

    suspend fun getEnrollmentsStatus(): List<EnrollmentStatus> {
        val response = api.getEnrollmentsStatus(corePreferences.user?.username ?: "")
        return response.map { it.mapToDomain() }
    }

    suspend fun getCourseDates(courseId: String) = api.getCourseDates(courseId)

}
