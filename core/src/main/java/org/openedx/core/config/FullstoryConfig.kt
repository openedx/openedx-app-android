package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class FullstoryConfig(
    @SerializedName("ENABLED")
    val isEnabled: Boolean = false,

    @SerializedName("ORG_ID")
    private val orgId: String = ""
)
