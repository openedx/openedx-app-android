package org.openedx.discussion.data.model.response

import com.google.gson.annotations.SerializedName
import org.openedx.discussion.domain.model.TopicsData

data class TopicsResponse(
    @SerializedName("courseware_topics")
    val coursewareTopics: List<Topic>?,
    @SerializedName("non_courseware_topics")
    val nonCoursewareTopics: List<Topic>?
) {

    data class Topic(
        @SerializedName("id")
        val id: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("thread_list_url")
        val threadListUrl: String?,
        @SerializedName("children")
        val children: List<Topic>?
    ) {
        fun mapToDomain(): org.openedx.discussion.domain.model.Topic {
            return org.openedx.discussion.domain.model.Topic(
                id = id ?: "",
                name = name ?: "",
                threadListUrl = threadListUrl ?: "",
                children = children?.map { it.mapToDomain() } ?: emptyList()
            )
        }
    }

    fun mapToDomain(): TopicsData {
        return TopicsData(
            coursewareTopics = coursewareTopics?.map { it.mapToDomain() } ?: emptyList(),
            nonCoursewareTopics = nonCoursewareTopics?.map { it.mapToDomain() } ?: emptyList()
        )
    }
}
