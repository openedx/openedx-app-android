package com.raccoongang.discovery.data.repository

import com.raccoongang.core.data.api.CourseApi
import com.raccoongang.core.data.model.room.CourseEntity
import com.raccoongang.core.domain.model.CourseList
import com.raccoongang.core.domain.model.Course
import com.raccoongang.discovery.data.storage.DiscoveryDao


class DiscoveryRepository(
    private val api: CourseApi,
    private val dao: DiscoveryDao,
) {

    suspend fun getCoursesList(
        username: String?,
        organization: String?,
        pageNumber: Int,
    ): CourseList {
        val pageResponse = api.getCourseList(
            page = pageNumber,
            mobile = true,
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
        val pageResponse = api.getCourseList(searchQuery = query, page = pageNumber, mobile = true)
        return CourseList(
            pageResponse.pagination.mapToDomain(),
            pageResponse.results?.map { it.mapToDomain() } ?: emptyList()
        )
    }
}