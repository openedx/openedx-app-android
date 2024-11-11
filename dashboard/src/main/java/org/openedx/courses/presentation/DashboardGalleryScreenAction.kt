package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse

interface DashboardGalleryScreenAction {
    object SwipeRefresh : DashboardGalleryScreenAction
    object ViewAll : DashboardGalleryScreenAction
    object Reload : DashboardGalleryScreenAction
    object NavigateToDiscovery : DashboardGalleryScreenAction
    data class OpenBlock(val enrolledCourse: EnrolledCourse, val blockId: String) : DashboardGalleryScreenAction
    data class OpenCourse(val enrolledCourse: EnrolledCourse) : DashboardGalleryScreenAction
    data class NavigateToDates(val enrolledCourse: EnrolledCourse) : DashboardGalleryScreenAction
}
