package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class ProgramConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,
    @SerializedName("PROGRAM_URL")
    val programUrl: String = "",
    @SerializedName("PROGRAM_DETAIL_URL_TEMPLATE")
    val programDetailUrlTemplate: String = "",
)
