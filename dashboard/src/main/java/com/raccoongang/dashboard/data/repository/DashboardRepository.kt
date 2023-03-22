package com.raccoongang.dashboard.data.repository

import com.raccoongang.core.data.api.CourseApi
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.dashboard.data.DashboardDao

class DashboardRepository(
    private val api: CourseApi,
    private val dao: DashboardDao,
    private val preferencesManager: PreferencesManager
) {

    suspend fun getEnrolledCourses(): List<EnrolledCourse> {
        val user = preferencesManager.user
        val result = api.getEnrolledCourses(
            username = user?.username ?: ""
        )
        dao.clearCachedData()
        dao.insertEnrolledCourseEntity(*result.map { it.mapToRoomEntity() }.toTypedArray())
        return result.map {
            it.mapToDomain()
        }
    }

    suspend fun getEnrolledCoursesFromCache(): List<EnrolledCourse> {
        val list = dao.readAllData()
        return list.map { it.mapToDomain() }
    }
}