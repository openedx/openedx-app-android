package org.openedx.app.deeplink

class DeepLink(params: Map<String, String>) {

    private val screenName = params[Keys.SCREEN_NAME.value]
    private val notificationType = params[Keys.NOTIFICATION_TYPE.value]
    val courseId = params[Keys.COURSE_ID.value]
    val pathId = params[Keys.PATH_ID.value]
    val componentId = params[Keys.COMPONENT_ID.value]
    val topicId = params[Keys.TOPIC_ID.value]
    val threadId = params[Keys.THREAD_ID.value]
    val commentId = params[Keys.COMMENT_ID.value]
    val parentId = params[Keys.PARENT_ID.value]
    val type = DeepLinkType.typeOf(screenName ?: notificationType ?: "")

    enum class Keys(val value: String) {
        SCREEN_NAME("screen_name"),
        NOTIFICATION_TYPE("notification_type"),
        COURSE_ID("course_id"),
        PATH_ID("path_id"),
        COMPONENT_ID("component_id"),
        TOPIC_ID("topic_id"),
        THREAD_ID("thread_id"),
        COMMENT_ID("comment_id"),
        PARENT_ID("parent_id"),
    }
}

enum class DeepLinkType(val type: String) {
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
    ENROLL("enroll"),
    UNENROLL("unenroll"),
    ADD_BETA_TESTER("add_beta_tester"),
    REMOVE_BETA_TESTER("remove_beta_tester"),
    FORUM_RESPONSE("forum_response"),
    FORUM_COMMENT("forum_comment"),
    NONE("");

    companion object {
        fun typeOf(type: String): DeepLinkType {
            return entries.firstOrNull { it.type == type } ?: NONE
        }
    }
}
