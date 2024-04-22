package org.openedx.core.config

import com.google.gson.annotations.SerializedName

enum class AnalyticsSource {
    @SerializedName("segment")
    SEGMENT,

    @SerializedName("none")
    NONE,
}
