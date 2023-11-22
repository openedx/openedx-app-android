package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class AgreementUrlsConfig(
    @SerializedName("PRIVACY_POLICY_URL")
    val privacyPolicyUrl: String = "",

    @SerializedName("TOS_URL")
    val tosUrl: String = "",

    @SerializedName("CONTACT_US_URL")
    val contactUsUrl: String = "",
)
