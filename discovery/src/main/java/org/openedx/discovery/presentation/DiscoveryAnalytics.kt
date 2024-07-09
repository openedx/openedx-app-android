package org.openedx.discovery.presentation

interface DiscoveryAnalytics {
    fun discoverySearchBarClickedEvent()
    fun discoveryCourseSearchEvent(label: String, coursesCount: Int)
    fun discoveryCourseClickedEvent(courseId: String, courseName: String)
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class DiscoveryAnalyticsEvent(val eventName: String, val biValue: String) {
    COURSE_INFO(
        "Discovery:Course Info",
        "edx.bi.app.discovery.course_info"
    ),
    PROGRAM_INFO(
        "Discovery:Program Info",
        "edx.bi.app.discovery.program_info"
    ),
    COURSE_ENROLL_CLICKED(
        "Discovery:Course Enroll Clicked",
        "edx.bi.app.course.enroll.clicked"
    ),
    COURSE_ENROLL_SUCCESS(
        "Discovery:Course Enroll Success",
        "edx.bi.app.course.enroll.success"
    ),
}

enum class DiscoveryAnalyticsKey(val key: String) {
    NAME("name"),
    COURSE_ID("course_id"),
    COURSE_NAME("course_name"),
    CATEGORY("category"),
    CONVERSION("conversion"),
    DISCOVERY("discovery"),
}

enum class DiscoveryAnalyticsScreen(val screenName: String) {
    DISCOVERY("Discovery"),
    PROGRAM("Program"),
    COURSE_INFO("Course Info"),
}
