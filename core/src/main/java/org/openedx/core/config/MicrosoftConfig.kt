package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class MicrosoftConfig(
    @SerializedName("ENABLED")
    private val enabled: Boolean = false,
    @SerializedName("CLIENT_ID")
    val clientId: String = "",
    @SerializedName("PACKAGE_SIGNATURE")
    val packageSignature: String = "",
) {
    fun isEnabled() = enabled && clientId.isNotBlank() && packageSignature.isNotBlank()
}
