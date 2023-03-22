package com.raccoongang.core.data.storage

import android.content.Context
import com.google.gson.Gson
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.domain.model.User
import com.raccoongang.core.domain.model.VideoSettings


class PreferencesManager(private val context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences("com.raccoongang.newedx", Context.MODE_PRIVATE)

    private fun saveString(key: String, value: String) {
        sharedPreferences.edit().apply {
            putString(key, value)
        }.apply()
    }

    private fun getString(key: String): String = sharedPreferences.getString(key, "") ?: ""

    fun clear() {
        sharedPreferences.edit().apply {
            remove(ACCESS_TOKEN)
            remove(REFRESH_TOKEN)
            remove(USER)
        }.apply()
    }


    var accessToken: String
        set(value) {
            saveString(ACCESS_TOKEN, value)
        }
        get() = getString(ACCESS_TOKEN)

    var refreshToken: String
        set(value) {
            saveString(REFRESH_TOKEN, value)
        }
        get() = getString(REFRESH_TOKEN)

    var user: User?
        set(value) {
            val userJson = Gson().toJson(value)
            saveString(USER, userJson)
        }
        get() {
            val userString = getString(USER)
            return Gson().fromJson(userString, User::class.java)
        }

    var profile: Account?
        set(value) {
            val accountJson = Gson().toJson(value)
            saveString(ACCOUNT, accountJson)
        }
        get() {
            val accountString = getString(ACCOUNT)
            return Gson().fromJson(accountString, Account::class.java)
        }

    var videoSettings: VideoSettings
        set(value) {
            val videoSettingsJson = Gson().toJson(value)
            saveString(VIDEO_SETTINGS, videoSettingsJson)
        }
        get() {
            val videoSettingsString = getString(VIDEO_SETTINGS)
            return Gson().fromJson(videoSettingsString, VideoSettings::class.java)
                ?: VideoSettings.default
        }


    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER = "user"
        private const val ACCOUNT = "account"
        private const val VIDEO_SETTINGS = "video_settings"
    }
}