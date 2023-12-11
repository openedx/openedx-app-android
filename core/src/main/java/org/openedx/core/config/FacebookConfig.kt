package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class FacebookConfig(
    @SerializedName("ENABLED")
    private val enabled: Boolean = false,
    @SerializedName("FACEBOOK_APP_ID")
    val appId: String = "",
    @SerializedName("CLIENT_TOKEN")
    val clientToken: String = "",
) {
    fun isEnabled() = enabled && appId.isNotBlank() && clientToken.isNotBlank()
}
