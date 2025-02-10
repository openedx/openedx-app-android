package org.openedx.dashboard.domain.interactor

import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.dashboard.data.repository.DashboardRepository
import org.openedx.dashboard.domain.CourseStatusFilter

class DashboardInteractor(
    private val repository: DashboardRepository,
) {

    suspend fun getEnrolledCourses(page: Int): DashboardCourseList {
        return repository.getEnrolledCourses(page)
    }

    suspend fun getEnrolledCoursesFromCache() = repository.getEnrolledCoursesFromCache()

    suspend fun getMainUserCourses(pageSize: Int) = repository.getMainUserCourses(pageSize)

    suspend fun getAllUserCourses(
        page: Int = 1,
        status: CourseStatusFilter? = null,
    ): DashboardCourseList {
        return repository.getAllUserCourses(
            page,
            status
        )
    }
}
