package org.openedx.discussion.domain.model

data class TopicsData(
    val coursewareTopics: List<Topic>,
    val nonCoursewareTopics: List<Topic>
)

data class Topic(
    val id: String,
    val name: String,
    val threadListUrl: String,
    val children: List<Topic>
)
