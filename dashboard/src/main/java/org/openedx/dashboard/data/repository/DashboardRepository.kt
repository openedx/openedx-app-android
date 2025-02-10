package org.openedx.dashboard.data.repository

import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseEnrollments
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.dashboard.data.DashboardDao
import org.openedx.dashboard.domain.CourseStatusFilter
import org.openedx.foundation.utils.FileUtil

class DashboardRepository(
    private val api: CourseApi,
    private val dao: DashboardDao,
    private val preferencesManager: CorePreferences,
    private val fileUtil: FileUtil,
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        val user = preferencesManager.user
        val result = api.getEnrolledCourses(
            username = user?.username ?: "",
            page = page
        )
        preferencesManager.appConfig = result.configs.mapToDomain()

        if (page == 1) dao.clearCachedData()
        dao.insertEnrolledCourseEntity(
            *result.enrollments.results.map { it.mapToRoomEntity() }
                .toTypedArray()
        )
        return result.enrollments.mapToDomain()
    }

    suspend fun getEnrolledCoursesFromCache(): List<EnrolledCourse> {
        val list = dao.readAllData()
        return list.map { it.mapToDomain() }
    }

    suspend fun getMainUserCourses(pageSize: Int): CourseEnrollments {
        val result = api.getUserCourses(
            username = preferencesManager.user?.username ?: "",
            pageSize = pageSize
        )
        preferencesManager.appConfig = result.configs.mapToDomain()

        fileUtil.saveObjectToFile(result)
        return result.mapToDomain()
    }

    suspend fun getAllUserCourses(page: Int, status: CourseStatusFilter?): DashboardCourseList {
        val user = preferencesManager.user
        val result = api.getUserCourses(
            username = user?.username ?: "",
            page = page,
            status = status?.key,
            fields = listOf("course_progress")
        )
        preferencesManager.appConfig = result.configs.mapToDomain()

        dao.clearCachedData()
        dao.insertEnrolledCourseEntity(
            *result.enrollments.results
                .map { it.mapToRoomEntity() }
                .toTypedArray()
        )
        return result.enrollments.mapToDomain()
    }
}
