package org.openedx.app.deeplink

enum class Screen(val screenName: String) {
    DISCOVERY("discovery"),
    DISCOVERY_COURSE_DETAIL("discovery_course_detail"),
    DISCOVERY_PROGRAM_DETAIL("discovery_program_detail"),
    COURSE_DASHBOARD("course_dashboard"),
    COURSE_VIDEOS("course_videos"),
    COURSE_DISCUSSION("course_discussion"),
    COURSE_DATES("course_dates"),
    COURSE_HANDOUT("course_handout"),
    COURSE_ANNOUNCEMENT("course_announcement"),
    COURSE_COMPONENT("course_component"),
    PROGRAM("program"),
    DISCUSSION_TOPIC("discussion_topic"),
    DISCUSSION_POST("discussion_post"),
    DISCUSSION_COMMENT("discussion_comment"),
    PROFILE("profile"),
    USER_PROFILE("user_profile"),
}
