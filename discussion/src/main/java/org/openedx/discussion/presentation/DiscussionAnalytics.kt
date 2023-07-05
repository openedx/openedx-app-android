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
}