package org.openedx.core.presentation

interface CoreAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class CoreAnalyticsEvent(val eventName: String, val biValue: String) {
    EXTERNAL_LINK_OPENING_ALERT(
        "External:Link Opening Alert",
        "edx.bi.app.discovery.external_link.opening.alert"
    ),
    EXTERNAL_LINK_OPENING_ALERT_ACTION(
        "External:Link Opening Alert Action",
        "edx.bi.app.discovery.external_link.opening.alert_action"
    ),
    VIDEO_BULK_DOWNLOAD_TOGGLE(
        "Video:Bulk Download Toggle",
        "edx.bi.app.videos.download.toggle"
    ),
    VIDEO_DOWNLOAD_SUBSECTION(
        "Video:Download Subsection",
        "edx.bi.video.download.subsection"
    ),
    VIDEO_DELETE_SUBSECTION(
        "Videos:Delete Subsection",
        "edx.bi.app.video.delete.subsection"
    ),
    VIDEO_STREAMING_QUALITY_CHANGED(
        "Video:Streaming Quality Changed",
        "edx.bi.app.video.streaming_quality.changed"
    ),
    VIDEO_DOWNLOAD_QUALITY_CHANGED(
        "Video:Download Quality Changed",
        "edx.bi.app.video.download_quality.changed"
    ),
}

enum class CoreAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    DISCOVERY("discovery"),
    VIDEOS("videos"),
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
    NUMBER_OF_VIDEOS("number_of_videos"),
}

enum class CoreAnalyticsScreen(val screenName: String) {
    COURSE_DATES("Course Dates"),
}
