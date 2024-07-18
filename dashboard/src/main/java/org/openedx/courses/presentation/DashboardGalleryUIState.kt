package org.openedx.courses.presentation

import org.openedx.core.domain.model.CourseEnrollments

sealed class DashboardGalleryUIState {
    data class Courses(val userCourses: CourseEnrollments, val useRelativeDates: Boolean) : DashboardGalleryUIState()
    data object Empty : DashboardGalleryUIState()
    data object Loading : DashboardGalleryUIState()
}
