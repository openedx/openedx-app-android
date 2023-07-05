package org.openedx.dashboard.data.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.PreferencesManager
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.dashboard.data.DashboardDao

class DashboardRepository(
    private val api: CourseApi,
    private val dao: DashboardDao,
    private val preferencesManager: PreferencesManager
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        val user = preferencesManager.user
        val result = api.getEnrolledCourses(
            username = user?.username ?: "",
            page = page
        )
        if (page == 1) dao.clearCachedData()
        dao.insertEnrolledCourseEntity(*result.results.map { it.mapToRoomEntity() }.toTypedArray())
        return result.mapToDomain()
    }

    suspend fun getEnrolledCoursesFromCache(): List<EnrolledCourse> {
        val list = dao.readAllData()
        return list.map { it.mapToDomain() }
    }
}