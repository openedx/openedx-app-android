package org.openedx.core.presentation

interface CoreAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class CoreAnalyticsEvent(val event: String) {
    EXTERNAL_LINK_OPENING_ALERT("External:Link Opening Alert"),
    EXTERNAL_LINK_OPENING_ALERT_ACTION("External:Link Opening Alert Action"),
    VIDEO_BULK_DOWNLOAD_TOGGLE("Video:Bulk Download Toggle"),
    VIDEO_DOWNLOAD_SUBSECTION("Video:Download Subsection"),
    VIDEO_DELETE_SUBSECTION("Videos:Delete Subsection"),
    VIDEO_STREAMING_QUALITY_CHANGED("Video:Streaming Quality Changed"),
    VIDEO_DOWNLOAD_QUALITY_CHANGED("Video:Download Quality Changed"),
}

enum class CoreAnalyticsValue(val biValue: String) {
    EXTERNAL_LINK_OPENING_ALERT("edx.bi.app.discovery.external_link.opening.alert"),
    EXTERNAL_LINK_OPENING_ALERT_ACTION("edx.bi.app.discovery.external_link.opening.alert_action"),
    VIDEO_BULK_DOWNLOAD_TOGGLE("edx.bi.app.videos.download.toggle"),
    VIDEO_DOWNLOAD_SUBSECTION("edx.bi.video.download.subsection"),
    VIDEO_DELETE_SUBSECTION("edx.bi.app.video.delete.subsection"),
    VIDEO_STREAMING_QUALITY_CHANGED("edx.bi.app.video.streaming_quality.changed"),
    VIDEO_DOWNLOAD_QUALITY_CHANGED("edx.bi.app.video.download_quality.changed"),
}

enum class CoreAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    DISCOVERY("discovery"),
    PROFILE("profile"),
    URL("url"),
    ACTION("action"),
    CANCEL("cancel"),
    CONTINUE("continue"),
    SCREEN_NAME("screen_name"),
    VALUE("value"),
    OLD_VALUE("old_value"),
    COURSE_ID("course_id"),
    BLOCK_ID("block_id"),
}

enum class CoreAnalyticsScreen(val screenName: String) {
    DISCOVERY("Discovery"),
    PROGRAM("Program"),
    COURSE_INFO("Course Info"),
    COURSE_DATES("Course Dates"),
}
