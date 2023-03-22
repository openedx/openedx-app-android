package com.raccoongang.auth.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("access_token")
    var accessToken: String?,
    @SerializedName("token_type")
    var tokenType: String?,
    @SerializedName("expires_in")
    var expiresIn: Long?,
    @SerializedName("scope")
    var scope: String?,
    @SerializedName("error")
    var error: String?,
    @SerializedName("refresh_token")
    var refreshToken: String?,
)

