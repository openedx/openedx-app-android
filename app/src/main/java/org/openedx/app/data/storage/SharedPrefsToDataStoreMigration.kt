package org.openedx.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.openedx.app.BuildConfig

class SharedPrefsToDataStoreMigration(
    private val context: Context,
) : DataMigration<Preferences> {

    private fun getOldPrefs(): SharedPreferences =
        context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        getOldPrefs().all.isNotEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val oldPrefs = getOldPrefs()
        val encryption = DataStoreEncryption()

        return currentData.toMutablePreferences().apply {
            migrateEncryptedStrings(oldPrefs, encryption)
            migrateLongs(oldPrefs)
            migrateStrings(oldPrefs)
            migrateBooleans(oldPrefs)
        }
    }

    private fun MutablePreferences.migrateEncryptedStrings(
        oldPrefs: SharedPreferences,
        encryption: DataStoreEncryption,
    ) {
        val key = ::stringPreferencesKey
        oldPrefs.migrateEncrypted(this, encryption, "access_token", key("access_token"))
        oldPrefs.migrateEncrypted(this, encryption, "refresh_token", key("refresh_token"))
        oldPrefs.migrateEncrypted(this, encryption, "push_token", key("push_token"))
        oldPrefs.migrateEncrypted(this, encryption, "user", key("user"))
        oldPrefs.migrateEncrypted(this, encryption, "account", key("account"))
    }

    private fun MutablePreferences.migrateLongs(oldPrefs: SharedPreferences) {
        val key = ::longPreferencesKey
        oldPrefs.migrateLong(this, "expires_in", key("expires_in"))
        oldPrefs.migrateLong(this, "CALENDAR_ID", key("calendar_id"))
    }

    private fun MutablePreferences.migrateStrings(oldPrefs: SharedPreferences) {
        val key = ::stringPreferencesKey
        oldPrefs.migrateString(
            this,
            "video_settings_streaming_quality",
            key("video_settings_streaming_quality")
        )
        oldPrefs.migrateString(
            this,
            "video_settings_download_quality",
            key("video_settings_download_quality")
        )
        oldPrefs.migrateString(this, "app_config", key("app_config"))
        oldPrefs.migrateString(this, "last_whats_new_version", key("last_whats_new_version"))
        oldPrefs.migrateString(this, "last_review_version", key("last_review_version"))
        oldPrefs.migrateString(this, "CALENDAR_USER", key("calendar_user"))
    }

    private fun MutablePreferences.migrateBooleans(oldPrefs: SharedPreferences) {
        val key = ::booleanPreferencesKey
        oldPrefs.migrateBoolean(
            this,
            "video_settings_wifi_download_only",
            key("video_settings_wifi_download_only"),
            true,
        )
        oldPrefs.migrateBoolean(
            this,
            "reset_app_directory",
            key("reset_app_directory"),
            true,
        )
        oldPrefs.migrateBoolean(
            this,
            "app_was_positive_rated",
            key("app_was_positive_rated"),
        )
        oldPrefs.migrateBoolean(
            this,
            "IS_RELATIVE_DATES_ENABLED",
            key("is_relative_dates_enabled"),
            true,
        )
        oldPrefs.migrateBoolean(
            this,
            "IS_CALENDAR_SYNC_ENABLED",
            key("is_calendar_sync_enabled"),
            true,
        )
        oldPrefs.migrateBoolean(
            this,
            "HIDE_INACTIVE_COURSES",
            key("hide_inactive_courses"),
            true,
        )
    }

    override suspend fun cleanUp() {
        getOldPrefs().edit().clear().commit()
    }

    private fun SharedPreferences.migrateEncrypted(
        prefs: MutablePreferences,
        encryption: DataStoreEncryption,
        oldKey: String,
        newKey: Preferences.Key<String>,
    ) {
        getString(oldKey, null)?.takeIf { it.isNotEmpty() }?.let {
            prefs[newKey] = encryption.encrypt(it)
        }
    }

    private fun SharedPreferences.migrateString(
        prefs: MutablePreferences,
        oldKey: String,
        newKey: Preferences.Key<String>,
    ) {
        getString(oldKey, null)?.let { prefs[newKey] = it }
    }

    private fun SharedPreferences.migrateLong(
        prefs: MutablePreferences,
        oldKey: String,
        newKey: Preferences.Key<Long>,
    ) {
        if (contains(oldKey)) prefs[newKey] = getLong(oldKey, 0L)
    }

    private fun SharedPreferences.migrateBoolean(
        prefs: MutablePreferences,
        oldKey: String,
        newKey: Preferences.Key<Boolean>,
        defValue: Boolean = false,
    ) {
        if (contains(oldKey)) prefs[newKey] = getBoolean(oldKey, defValue)
    }
}
