package org.openedx.app.deeplink

class DeepLink(params: Map<String, String>) {

    val screenName = params[Keys.SCREEN_NAME.value]
    val courseId = params[Keys.COURSE_ID.value]
    val pathId = params[Keys.PATH_ID.value]
    val componentId = params[Keys.COMPONENT_ID.value]
    val topicId = params[Keys.TOPIC_ID.value]
    val threadId = params[Keys.THREAD_ID.value]
    val commentId = params[Keys.COMMENT_ID.value]

    enum class Keys(val value: String) {
        SCREEN_NAME("screen_name"),
        COURSE_ID("course_id"),
        PATH_ID("path_id"),
        COMPONENT_ID("component_id"),
        TOPIC_ID("topic_id"),
        THREAD_ID("thread_id"),
        COMMENT_ID("comment_id")
    }
}
