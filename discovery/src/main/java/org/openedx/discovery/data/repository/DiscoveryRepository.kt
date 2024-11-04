package org.openedx.discovery.data.repository

import okhttp3.ResponseBody
import org.openedx.core.data.model.EnrollBody
import org.openedx.core.data.storage.CorePreferences
import org.openedx.discovery.data.api.DiscoveryApi
import org.openedx.discovery.data.model.room.CourseEntity
import org.openedx.discovery.data.storage.DiscoveryDao
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.domain.model.CourseList

class DiscoveryRepository(
    private val api: DiscoveryApi,
    private val dao: DiscoveryDao,
    private val preferencesManager: CorePreferences,
) {

    suspend fun getCourseDetail(id: String): Course {
        val course = api.getCourseDetail(id, preferencesManager.user?.username)
        dao.updateCourseEntity(CourseEntity.createFrom(course))
        return course.mapToDomain()
    }

    suspend fun getCourseDetailFromCache(id: String): Course? {
        return dao.getCourseById(id)?.mapToDomain()
    }

    suspend fun enrollInACourse(courseId: String): ResponseBody {
        val enrollBody = EnrollBody(
            EnrollBody.CourseDetails(
                courseId = courseId,
                emailOptIn = preferencesManager.user?.email
            )
        )
        return api.enrollInACourse(enrollBody)
    }

    suspend fun getCoursesList(
        username: String?,
        organization: String?,
        pageNumber: Int,
    ): CourseList {
        val pageResponse = api.getCourseList(
            page = pageNumber,
            mobile = true,
            mobileSearch = false,
            username = username,
            org = organization
        )
        if (pageNumber == 1) dao.clearCachedData()
        val cachedDataList =
            pageResponse.results?.map { CourseEntity.createFrom(it) } ?: emptyList()
        dao.insertCourseEntity(*cachedDataList.toTypedArray())
        return CourseList(
            pageResponse.pagination.mapToDomain(),
            pageResponse.results?.map { it.mapToDomain() } ?: emptyList()
        )
    }

    suspend fun getCachedCoursesList(): List<Course> {
        val dataFromDb = dao.readAllData()
        return dataFromDb.map { it.mapToDomain() }
    }

    suspend fun getCoursesListByQuery(
        query: String,
        pageNumber: Int,
    ): CourseList {
        val pageResponse = api.getCourseList(
            searchQuery = query,
            page = pageNumber,
            mobile = true,
            mobileSearch = true
        )
        return CourseList(
            pageResponse.pagination.mapToDomain(),
            pageResponse.results?.map { it.mapToDomain() } ?: emptyList()
        )
    }
}
