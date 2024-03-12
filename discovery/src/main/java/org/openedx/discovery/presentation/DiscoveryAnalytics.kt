package org.openedx.discovery.presentation

interface DiscoveryAnalytics {
    fun discoverySearchBarClickedEvent()
    fun discoveryCourseSearchEvent(label: String, coursesCount: Int)
    fun discoveryCourseClickedEvent(courseId: String, courseName: String)
    fun logEvent(event: String, params: Map<String, Any?>)
}