package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class BranchConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName("KEY")
    val key: String = "",

    @SerializedName("URI_SCHEME")
    val uriScheme: String = "",

    @SerializedName("HOST")
    val host: String = "",

    @SerializedName("ALTERNATE_HOST")
    val alternateHost: String = "",
)
