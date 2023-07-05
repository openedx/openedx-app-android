package org.openedx.dashboard.domain.interactor

import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.dashboard.data.repository.DashboardRepository

class DashboardInteractor(
    private val repository: DashboardRepository
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        return repository.getEnrolledCourses(page)
    }

    suspend fun getEnrolledCoursesFromCache() = repository.getEnrolledCoursesFromCache()
}