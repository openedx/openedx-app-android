package org.openedx.auth.data.model

import com.google.gson.annotations.SerializedName

data class PasswordResetResponse(
    @SerializedName("success")
    val success: Boolean
)
