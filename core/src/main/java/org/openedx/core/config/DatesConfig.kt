package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class DatesConfig(
    @SerializedName("ENABLED")
    val isEnabled: Boolean = true,
)
