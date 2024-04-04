package org.openedx.settings.data.repository

import androidx.room.RoomDatabase
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.settings.data.api.SettingsApi

class SettingsRepository(
    private val config: Config,
    private val api: SettingsApi,
    private val room: RoomDatabase,
    private val corePreferences: CorePreferences,
) {

    suspend fun logout() {
        try {
            api.revokeAccessToken(
                config.getOAuthClientId(),
                corePreferences.refreshToken,
                ApiConstants.TOKEN_TYPE_REFRESH
            )
        } finally {
            corePreferences.clear()
            room.clearAllTables()
        }
    }
}
