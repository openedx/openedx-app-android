package org.openedx.discussion.data.model.request

import com.google.gson.annotations.SerializedName

data class FollowBody(
    @SerializedName("following")
    val following: Boolean
)
