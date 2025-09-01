package org.openedx.auth.data.repository

import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.data.model.ValidationFields
import org.openedx.auth.domain.model.AuthResponse
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.EdxError
import android.util.Log

class AuthRepository(
    private val config: Config,
    private val api: AuthApi,
    private val preferencesManager: CorePreferences,
) {

    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.getAccessToken(
                grantType = "password",
                clientId = "android",
                username = username,
                password = password,
                tokenType = "JWT",            // from your config (TOKEN_TYPE in JSON)
                isAsymmetricJwt = true        // default, you can omit if always true
            )
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.mapToDomain())
                } ?: Result.failure(Exception("Empty response"))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun socialLogin(token: String?, authType: AuthType) {
        require(!token.isNullOrBlank()) { "Token is null" }
        api.exchangeAccessToken(
            accessToken = token,
            clientId = config.getOAuthClientId(),
            tokenType = config.getAccessTokenType(),
            authType = authType.postfix
        )
            .mapToDomain()
            .processAuthResponse()
    }

    suspend fun browserAuthCodeLogin(code: String) {
        api.getAccessTokenFromCode(
            grantType = ApiConstants.GRANT_TYPE_CODE,
            clientId = config.getOAuthClientId(),
            code = code,
            redirectUri = "${config.getAppId()}://${ApiConstants.BrowserLogin.REDIRECT_HOST}",
            tokenType = config.getAccessTokenType(),
        ).mapToDomain().processAuthResponse()
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

    private suspend fun AuthResponse.processAuthResponse() {
        if (error != null) {
            throw EdxError.UnknownException(error!!)
        }
        preferencesManager.accessToken = accessToken ?: ""
        preferencesManager.refreshToken = refreshToken ?: ""
        preferencesManager.accessTokenExpiresAt = getTokenExpiryTime()
        val user = api.getProfile()
        preferencesManager.user = user
    }
}
