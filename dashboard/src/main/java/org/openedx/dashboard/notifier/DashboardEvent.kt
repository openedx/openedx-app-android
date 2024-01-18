package org.openedx.dashboard.notifier

import java.lang.Exception

sealed class DashboardEvent {
    object Empty : DashboardEvent()
    object NavigationToDiscovery : DashboardEvent()
    class CourseEnrolled(val courseId: String) : DashboardEvent()
    class CourseEnrolledSuccess(var courseId: String) : DashboardEvent()
    class CourseEnrolledError(val exception: Exception) : DashboardEvent()
}
