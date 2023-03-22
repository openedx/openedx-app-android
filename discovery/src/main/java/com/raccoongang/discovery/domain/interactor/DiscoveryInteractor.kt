package com.raccoongang.discovery.domain.interactor

import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.domain.model.CourseList
import com.raccoongang.discovery.data.repository.DiscoveryRepository

class DiscoveryInteractor(private val repository: DiscoveryRepository) {

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