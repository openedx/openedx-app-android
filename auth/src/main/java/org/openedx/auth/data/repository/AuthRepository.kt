package org.openedx.auth.data.repository

import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.data.model.LoginType
import org.openedx.auth.data.model.ValidationFields
import org.openedx.auth.domain.model.AuthResponse
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.EdxError

class AuthRepository(
    private val config: Config,
    private val api: AuthApi,
    private val preferencesManager: CorePreferences,
) {

    suspend fun login(
        username: String,
        password: String,
    ) {
        val authResponse: AuthResponse = api.getAccessToken(
            ApiConstants.GRANT_TYPE_PASSWORD,
            config.getOAuthClientId(),
            username,
            password,
            config.getAccessTokenType(),
        ).mapToDomain()
        if (authResponse.error != null) {
            throw EdxError.UnknownException(authResponse.error!!)
        }
        preferencesManager.accessToken = authResponse.accessToken ?: ""
        preferencesManager.refreshToken = authResponse.refreshToken ?: ""
        preferencesManager.accessTokenExpiresAt = authResponse.getTokenExpiryTime()
        val user = api.getProfile()
        preferencesManager.user = user
    }

    suspend fun socialLogin(token: String?, loginType: LoginType) {
        if (token.isNullOrBlank()) throw IllegalArgumentException("Token is null")
        val authResponse = api.exchangeAccessToken(
            accessToken = token,
            clientId = config.getOAuthClientId(),
            tokenType = config.getAccessTokenType(),
            authType = loginType.postfix
        ).mapToDomain()
        if (authResponse.error != null) {
            throw EdxError.UnknownException(authResponse.error!!)
        }
        preferencesManager.accessToken = authResponse.accessToken ?: ""
        preferencesManager.refreshToken = authResponse.refreshToken ?: ""
        preferencesManager.accessTokenExpiresAt = authResponse.getTokenExpiryTime()
        val user = api.getProfile()
        preferencesManager.user = user
    }

    suspend fun getRegistrationFields(): List<RegistrationField> {
        return api.getRegistrationFields().fields?.map { it.mapToDomain() } ?: emptyList()
    }

    suspend fun register(mapFields: Map<String, String>) {
        return api.registerUser(mapFields)
    }

    suspend fun validateRegistrationFields(mapFields: Map<String, String>): ValidationFields {
        return api.validateRegistrationFields(mapFields)
    }

    suspend fun passwordReset(email: String): Boolean {
        return api.passwordReset(email).success
    }
}
