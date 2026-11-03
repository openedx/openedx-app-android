package org.openedx.core.data.storage

import org.openedx.core.data.model.User
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.VideoSettings

interface CorePreferences {
    var accessToken: String
    var refreshToken: String

    /** This device's push token. Device-wide: each signed-in site registers the same token. */
    var pushToken: String
    var accessTokenExpiresAt: Long
    var user: User?
    var videoSettings: VideoSettings
    var appConfig: AppConfig
    var canResetAppDirectory: Boolean
    var isRelativeDatesEnabled: Boolean

    /** Clears the selected instance's session: tokens, user and profile. */
    suspend fun clearCorePreferences()

    /** Whether [instanceKey]'s site has a stored session, without selecting it. */
    fun hasSession(instanceKey: String?): Boolean

    /** Clears one instance's session without touching the selected one. */
    suspend fun clearSession(instanceKey: String?)

    fun accessToken(instanceKey: String?): String

    fun refreshToken(instanceKey: String?): String

    /** Stores refreshed tokens for the site that issued them, whichever site is selected now. */
    fun saveTokens(instanceKey: String?, accessToken: String, refreshToken: String, expiresAt: Long)

    fun saveAccessToken(instanceKey: String?, accessToken: String)

    fun saveUser(instanceKey: String?, user: User?)

    /** Who signed in to [instanceKey]'s site last. Survives logout, unlike [user]. */
    fun lastSignedInUsername(instanceKey: String?): String

    fun setLastSignedInUsername(instanceKey: String?, username: String)
}
