package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class GoogleConfig(
    @SerializedName("ENABLED")
    private val enabled: Boolean = false,
    @SerializedName("CLIENT_ID")
    val clientId: String = "",
) {
    fun isEnabled() = enabled && clientId.isNotBlank()
}
