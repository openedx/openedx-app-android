package org.openedx.auth.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.auth.domain.model.AuthResponse

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
) {
    fun mapToDomain(): AuthResponse {
        return AuthResponse(
            accessToken = accessToken,
            tokenType = tokenType,
            expiresIn = expiresIn?.times(1000),
            scope = scope,
            error = error,
            refreshToken = refreshToken,
        )
    }
}
