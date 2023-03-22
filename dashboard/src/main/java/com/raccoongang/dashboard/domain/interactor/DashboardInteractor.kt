package com.raccoongang.dashboard.domain.interactor

import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.dashboard.data.repository.DashboardRepository

class DashboardInteractor(
    private val repository: DashboardRepository
) {

    suspend fun getEnrolledCourses(): List<EnrolledCourse> {
        return repository.getEnrolledCourses()
    }

    suspend fun getEnrolledCoursesFromCache() = repository.getEnrolledCoursesFromCache()
}