package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class AppsFlyerConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName("DEV_KEY")
    val devKey: String = "",
)
