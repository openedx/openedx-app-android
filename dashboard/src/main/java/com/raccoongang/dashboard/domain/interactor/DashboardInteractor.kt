package com.raccoongang.dashboard.domain.interactor

import com.raccoongang.core.domain.model.DashboardCourseList
import com.raccoongang.dashboard.data.repository.DashboardRepository

class DashboardInteractor(
    private val repository: DashboardRepository
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        return repository.getEnrolledCourses(page)
    }

    suspend fun getEnrolledCoursesFromCache() = repository.getEnrolledCoursesFromCache()
}