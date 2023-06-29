package com.raccoongang.discovery.presentation

interface DiscoveryAnalytics {
    fun discoverySearchBarClickedEvent()
    fun discoveryCourseSearchEvent(label: String, coursesCount: Int)
    fun discoveryCourseClickedEvent(courseId: String, courseName: String)
}