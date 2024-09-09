package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class XBlockProgressBody(
    @SerializedName("body")
    val body: String
)
