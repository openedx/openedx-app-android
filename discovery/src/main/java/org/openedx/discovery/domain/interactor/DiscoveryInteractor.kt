package org.openedx.discovery.domain.interactor

import org.openedx.discovery.data.repository.DiscoveryRepository
import org.openedx.discovery.domain.model.Course
import org.openedx.discovery.domain.model.CourseList

class DiscoveryInteractor(private val repository: DiscoveryRepository) {

    suspend fun getCourseDetails(id: String) = repository.getCourseDetail(id)

    suspend fun getCourseDetailsFromCache(id: String) = repository.getCourseDetailFromCache(id)

    suspend fun enrollInACourse(id: String) {
        repository.enrollInACourse(courseId = id)
    }

    suspend fun getCoursesList(
        username: String?,
        organization: String?,
        pageNumber: Int
    ): CourseList {
        return repository.getCoursesList(username, organization, pageNumber)
    }

    suspend fun getCoursesListByQuery(
        query: String,
        pageNumber: Int,
    ) = repository.getCoursesListByQuery(query, pageNumber)

    suspend fun getCoursesListFromCache(): List<Course> {
        return repository.getCachedCoursesList()
    }
}
