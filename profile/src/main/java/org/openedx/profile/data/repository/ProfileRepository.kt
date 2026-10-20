package org.openedx.profile.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.RequestInstance
import org.openedx.profile.data.api.ProfileApi
import org.openedx.profile.data.storage.ProfilePreferences
import org.openedx.profile.domain.model.Account
import java.io.File

class ProfileRepository(
    private val config: Config,
    private val api: ProfileApi,
    private val profilePreferences: ProfilePreferences,
    private val corePreferences: CorePreferences,
    private val sessionManager: InstanceSessionManager,
    private val instanceProvider: InstanceProvider,
) {

    suspend fun getAccount(): Account {
        val account = api.getAccount(corePreferences.user?.username!!)
        profilePreferences.profile = account
        return account.mapToDomain()
    }

    suspend fun getAccount(username: String): Account {
        val account = api.getAccount(username)
        return account.mapToDomain()
    }

    fun getCachedAccount(): Account? {
        return profilePreferences.profile?.mapToDomain()
    }

    suspend fun updateAccount(fields: Map<String, Any?>): Account {
        return api.updateAccount(corePreferences.user?.username!!, fields).mapToDomain()
    }

    suspend fun setProfileImage(file: File, mimeType: String) {
        api.setProfileImage(
            corePreferences.user?.username!!,
            "attachment;filename=filename.${file.extension}",
            true,
            file.asRequestBody(mimeType.toMediaType())
        )
    }

    suspend fun deleteProfileImage() {
        api.deleteProfileImage(corePreferences.user?.username!!)
    }

    suspend fun deactivateAccount(password: String) = api.deactivateAccount(password)

    /**
     * Signs out of the site selected when it starts. The revoke request is pinned to that site,
     * and if the learner selects another site while it is in flight, only the original site is
     * signed out: the new selection and its session stay.
     */
    suspend fun logout() {
        val siteKey = instanceProvider.currentInstance?.key
        val clientId = config.getOAuthClientId()
        val refreshToken = corePreferences.refreshToken(siteKey)
        try {
            api.revokeAccessToken(
                clientId,
                refreshToken,
                ApiConstants.TOKEN_TYPE_REFRESH,
                RequestInstance(siteKey),
            )
        } finally {
            sessionManager.logout(siteKey)
        }
    }
}
