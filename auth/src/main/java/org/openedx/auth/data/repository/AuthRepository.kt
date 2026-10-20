package org.openedx.auth.data.repository

import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.data.model.ValidationFields
import org.openedx.auth.domain.model.AuthResponse
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.InstanceSwitchedException
import org.openedx.core.instance.RequestInstance
import org.openedx.core.system.EdxError

/**
 * Sign-in belongs to the site selected when it starts. Every request is pinned to that site,
 * every credential is written to that site's storage, and if the learner selects another site
 * before it finishes, it stops with [InstanceSwitchedException] instead of completing.
 */
class AuthRepository(
    private val config: Config,
    private val api: AuthApi,
    private val preferencesManager: CorePreferences,
    private val sessionManager: InstanceSessionManager,
    private val instanceProvider: InstanceProvider,
) {

    suspend fun login(
        username: String,
        password: String,
    ) {
        val site = currentSite()
        api.getAccessToken(
            grantType = ApiConstants.GRANT_TYPE_PASSWORD,
            clientId = config.getOAuthClientId(),
            username = username,
            password = password,
            tokenType = config.getAccessTokenType(),
            instance = site,
        )
            .mapToDomain()
            .processAuthResponse(site)
    }

    suspend fun ssoLogin(
        jwtToken: String
    ) {
        val site = currentSite()
        if (preferencesManager.accessToken(site.instanceKey).isBlank()) {
            preferencesManager.saveAccessToken(site.instanceKey, jwtToken)
        }
        saveProfile(site)
    }

    suspend fun socialLogin(token: String?, authType: AuthType) {
        require(!token.isNullOrBlank()) { "Token is null" }
        val site = currentSite()
        api.exchangeAccessToken(
            accessToken = token,
            clientId = config.getOAuthClientId(),
            tokenType = config.getAccessTokenType(),
            authType = authType.postfix,
            instance = site,
        )
            .mapToDomain()
            .processAuthResponse(site)
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

    private fun currentSite() = RequestInstance(instanceProvider.currentInstance?.key)

    private fun ensureStillSelected(site: RequestInstance) {
        if (instanceProvider.currentInstance?.key != site.instanceKey) {
            throw InstanceSwitchedException(site.instanceKey)
        }
    }

    private suspend fun AuthResponse.processAuthResponse(site: RequestInstance) {
        if (error != null) {
            throw EdxError.UnknownException(error!!)
        }
        ensureStillSelected(site)
        preferencesManager.saveTokens(
            site.instanceKey,
            accessToken.orEmpty(),
            refreshToken.orEmpty(),
            getTokenExpiryTime(),
        )
        saveProfile(site)
    }

    private suspend fun saveProfile(site: RequestInstance) {
        val user = api.getProfile(site)
        ensureStillSelected(site)
        preferencesManager.saveUser(site.instanceKey, user)
        user.username?.let { sessionManager.onSignedIn(site.instanceKey, it) }
    }
}
