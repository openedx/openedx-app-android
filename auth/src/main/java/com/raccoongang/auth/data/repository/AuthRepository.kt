package com.raccoongang.auth.data.repository

import com.raccoongang.auth.data.api.AuthApi
import com.raccoongang.auth.data.model.ValidationFields
import com.raccoongang.core.ApiConstants
import com.raccoongang.core.BuildConfig
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.system.EdxError

class AuthRepository(
    private val api: AuthApi,
    private val preferencesManager: PreferencesManager,
) {

    suspend fun login(
        username: String,
        password: String,
    ) {
        val authResponse = api.getAccessToken(
            ApiConstants.GRANT_TYPE_PASSWORD,
            BuildConfig.CLIENT_ID,
            username,
            password
        )
        if (authResponse.error != null) {
            throw EdxError.UnknownException(authResponse.error!!)
        }
        preferencesManager.accessToken = authResponse.accessToken ?: ""
        preferencesManager.refreshToken = authResponse.refreshToken ?: ""
        val user = api.getProfile().mapToDomain()
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