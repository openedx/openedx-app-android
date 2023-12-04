package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class SocialConfig(
    @SerializedName("GOOGLE_CLIENT_ID")
    val googleClientId: String = "",

    @SerializedName("FACEBOOK_APP_ID")
    val facebookAppId: String = "",

    @SerializedName("FACEBOOK_CLIENT_TOKEN")
    val facebookClientTokenId: String = "",

    @SerializedName("MICROSOFT_CLIENT_ID")
    val microsoftClientId: String = "",

    @SerializedName("MICROSOFT_PACKAGE_SIGNATURE")
    val microsoftPackageSignature: String = "",
) {
    fun isValidConfig() =
        googleClientId.isNotBlank() &&
                facebookAppId.isNotBlank() &&
                facebookClientTokenId.isNotBlank() &&
                microsoftClientId.isNotBlank() &&
                microsoftPackageSignature.isNotBlank()
}
