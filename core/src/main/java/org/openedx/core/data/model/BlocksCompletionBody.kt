package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class BlocksCompletionBody(
    @SerializedName("username")
    val username: String,
    @SerializedName("course_key")
    val courseId: String,
    @SerializedName("blocks")
    val blocks: Map<String, String>
)
