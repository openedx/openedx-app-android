package com.raccoongang.dashboard.data.repository

import com.raccoongang.core.data.api.CourseApi
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.DashboardCourseList
import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.dashboard.data.DashboardDao

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