package com.raccoongang.discussion.presentation

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