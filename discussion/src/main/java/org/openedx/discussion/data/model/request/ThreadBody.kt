package org.openedx.discussion.data.model.request

import com.google.gson.annotations.SerializedName

data class ThreadBody(
    @SerializedName("type")
    val type: String,
    @SerializedName("topic_id")
    val topicId: String,
    @SerializedName("course_id")
    val courseId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("raw_body")
    val rawBody: String,
    @SerializedName("following")
    val following: Boolean = true
)
