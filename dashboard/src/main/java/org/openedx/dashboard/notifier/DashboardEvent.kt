package org.openedx.dashboard.notifier

sealed class DashboardEvent {
    object NavigationToDiscovery : DashboardEvent()
    object UpdateEnrolledCourses : DashboardEvent()
}
