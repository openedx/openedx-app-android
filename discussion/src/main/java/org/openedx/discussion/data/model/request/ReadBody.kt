package org.openedx.discussion.data.model.request

import com.google.gson.annotations.SerializedName

data class ReadBody(
    @SerializedName("read")
    val read: Boolean
)
