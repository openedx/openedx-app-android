package org.openedx.dashboard.domain.interactor

import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.courses.domain.model.UserCourses
import org.openedx.dashboard.data.repository.DashboardRepository
import org.openedx.dashboard.domain.CourseStatusFilter

class DashboardInteractor(
    private val repository: DashboardRepository
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        return repository.getEnrolledCourses(page)
    }

    suspend fun getEnrolledCoursesFromCache() = repository.getEnrolledCoursesFromCache()

    suspend fun getUserCourses(page: Int = 1, status: CourseStatusFilter? = null): UserCourses {
        return repository.getUserCourses(
            page,
            status
        )
    }
}