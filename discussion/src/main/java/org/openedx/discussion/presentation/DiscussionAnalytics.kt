package org.openedx.discussion.presentation

interface DiscussionAnalytics {
    fun discussionAllPostsClickedEvent(courseId: String, courseName: String)
    fun discussionFollowingClickedEvent(courseId: String, courseName: String)
    fun discussionTopicClickedEvent(
        courseId: String,
        courseName: String,
        topicId: String,
        topicName: String
    )

    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class DiscussionAnalyticEvent(val eventName: String) {
    FORUM_ADD_RESPONSE_COMMENT("Forum:Add Response Comment"),
    FORUM_ADD_THREAD_RESPONSE("Forum:Add Thread Response"),
    FORUM_CREATE_TOPIC_THREAD("Forum:Create Topic Thread"),
    FORUM_SEARCH_THREADS("Forum:Search Threads"),
    FORUM_VIEW_RESPONSE_COMMENTS("Forum:View Response Comments"),
    FORUM_VIEW_THREAD("Forum:View Thread"),
    FORUM_VIEW_TOPIC_THREADS("Forum:View Topic Threads"),
    FORUM_VIEW_TOPICS("Forum:View Topics")
}

enum class DiscussionAnalyticValue(val value: String) {
    SCREEN_NAVIGATION("edx.bi.app.navigation.screen"),
    ALL_POSTS("all_posts"),
    POSTS_FOLLOWING("posts_following"),
    DISCUSSION_TOPIC("discussion_topic"),
}

enum class DiscussionAnalyticKey(val key: String) {
    COURSE_ID("course_id"),
    ACTION("action"),
    THREAD_ID("thread_id"),
    TOPIC_ID("topic_id"),
    RESPONSE_ID("response_id"),
    AUTHOR("author"),
    SEARCH_QUERY("search_query"),
}
