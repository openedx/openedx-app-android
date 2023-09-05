package org.openedx.auth.data.repository

import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.data.model.AuthResponse
import org.openedx.auth.data.model.ValidationFields
import org.openedx.core.ApiConstants
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.EdxError

class AuthRepository(
    private val api: AuthApi,
    private val preferencesManager: CorePreferences,
) {

    private suspend fun processAuthResponse(authResponse: AuthResponse) {
        if (authResponse.error != null) {
            throw EdxError.UnknownException(authResponse.error!!)
        }
        preferencesManager.accessToken = authResponse.accessToken ?: ""
        preferencesManager.refreshToken = authResponse.refreshToken ?: ""
        val user = api.getProfile().mapToDomain()
        preferencesManager.user = user
    }

    suspend fun login(
        username: String,
        password: String,
    ) {
        val authResponse = api.getAccessToken(
            ApiConstants.GRANT_TYPE_PASSWORD,
            org.openedx.core.BuildConfig.CLIENT_ID,
            username,
            password
        )
        processAuthResponse(authResponse)
    }

    suspend fun login(code: String) {
        val authResponse = api.getAccessToken(
            ApiConstants.GRANT_TYPE_CODE,
            org.openedx.core.BuildConfig.CLIENT_ID,
            code,
        )
        processAuthResponse(authResponse)
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