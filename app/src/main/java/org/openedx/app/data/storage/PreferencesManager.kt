package org.openedx.app.data.storage

import android.content.Context
import com.google.gson.Gson
import org.openedx.app.BuildConfig
import org.openedx.core.data.storage.CorePreferences
import org.openedx.profile.data.model.Account
import org.openedx.core.data.model.User
import org.openedx.core.domain.model.VideoSettings
import org.openedx.profile.data.storage.ProfilePreferences

class PreferencesManager(context: Context) : CorePreferences, ProfilePreferences {

    private val sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    private fun saveString(key: String, value: String) {
        sharedPreferences.edit().apply {
            putString(key, value)
        }.apply()
    }

    private fun getString(key: String): String = sharedPreferences.getString(key, "") ?: ""

    override fun clear() {
        sharedPreferences.edit().apply {
            remove(ACCESS_TOKEN)
            remove(REFRESH_TOKEN)
            remove(USER)
        }.apply()
    }

    override var accessToken: String
        set(value) {
            saveString(ACCESS_TOKEN, value)
        }
        get() = getString(ACCESS_TOKEN)

    override var refreshToken: String
        set(value) {
            saveString(REFRESH_TOKEN, value)
        }
        get() = getString(REFRESH_TOKEN)

    override var user: User?
        set(value) {
            val userJson = Gson().toJson(value)
            saveString(USER, userJson)
        }
        get() {
            val userString = getString(USER)
            return Gson().fromJson(userString, User::class.java)
        }

    override var profile: Account?
        set(value) {
            val accountJson = Gson().toJson(value)
            saveString(ACCOUNT, accountJson)
        }
        get() {
            val accountString = getString(ACCOUNT)
            return Gson().fromJson(accountString, Account::class.java)
        }

    override var videoSettings: VideoSettings
        set(value) {
            val videoSettingsJson = Gson().toJson(value)
            saveString(VIDEO_SETTINGS, videoSettingsJson)
        }
        get() {
            val videoSettingsString = getString(VIDEO_SETTINGS)
            return Gson().fromJson(videoSettingsString, VideoSettings::class.java)
                ?: VideoSettings.default
        }

    override var lastWhatsNewVersion: String
        set(value) {
            saveString(LAST_WHATS_NEW_VERSION, value)
        }
        get() = getString(LAST_WHATS_NEW_VERSION)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER = "user"
        private const val ACCOUNT = "account"
        private const val VIDEO_SETTINGS = "video_settings"
        private const val LAST_WHATS_NEW_VERSION = "last_whats_new_version"
    }
}