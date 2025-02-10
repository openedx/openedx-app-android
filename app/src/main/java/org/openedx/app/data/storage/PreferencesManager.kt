package org.openedx.app.data.storage

import android.content.Context
import com.google.gson.Gson
import org.openedx.app.BuildConfig
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.system.CalendarManager
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.foundation.extension.replaceSpace
import org.openedx.profile.data.model.Account
import org.openedx.profile.data.storage.ProfilePreferences
import org.openedx.whatsnew.data.storage.WhatsNewPreferences

class PreferencesManager(context: Context) :
    CorePreferences,
    ProfilePreferences,
    WhatsNewPreferences,
    InAppReviewPreferences,
    CoursePreferences,
    CalendarPreferences {

    private val sharedPreferences =
        context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    private fun saveString(key: String, value: String) {
        sharedPreferences.edit().apply {
            putString(key, value)
        }.apply()
    }

    private fun getString(key: String, defValue: String = ""): String {
        return sharedPreferences.getString(key, defValue) ?: defValue
    }

    private fun saveLong(key: String, value: Long) {
        sharedPreferences.edit().apply {
            putLong(key, value)
        }.apply()
    }

    private fun getLong(key: String, defValue: Long = 0): Long = sharedPreferences.getLong(key, defValue)

    private fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(key, value)
        }.apply()
    }

    private fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }

    override fun clearCorePreferences() {
        sharedPreferences.edit().apply {
            remove(ACCESS_TOKEN)
            remove(REFRESH_TOKEN)
            remove(USER)
            remove(ACCOUNT)
            remove(EXPIRES_IN)
        }.apply()
    }

    override fun clearCalendarPreferences() {
        sharedPreferences.edit().apply {
            remove(CALENDAR_ID)
            remove(IS_CALENDAR_SYNC_ENABLED)
            remove(HIDE_INACTIVE_COURSES)
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

    override var pushToken: String
        set(value) {
            saveString(PUSH_TOKEN, value)
        }
        get() = getString(PUSH_TOKEN)

    override var accessTokenExpiresAt: Long
        set(value) {
            saveLong(EXPIRES_IN, value)
        }
        get() = getLong(EXPIRES_IN)

    override var calendarId: Long
        set(value) {
            saveLong(CALENDAR_ID, value)
        }
        get() = getLong(CALENDAR_ID, CalendarManager.CALENDAR_DOES_NOT_EXIST)

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
            saveBoolean(VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY, value.wifiDownloadOnly)
            saveString(VIDEO_SETTINGS_STREAMING_QUALITY, value.videoStreamingQuality.name)
            saveString(VIDEO_SETTINGS_DOWNLOAD_QUALITY, value.videoDownloadQuality.name)
        }
        get() {
            val wifiDownloadOnly = getBoolean(VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY, defValue = true)
            val streamingQualityString =
                getString(VIDEO_SETTINGS_STREAMING_QUALITY, defValue = VideoQuality.AUTO.name)
            val downloadQualityString =
                getString(VIDEO_SETTINGS_DOWNLOAD_QUALITY, defValue = VideoQuality.AUTO.name)

            return VideoSettings(
                wifiDownloadOnly = wifiDownloadOnly,
                videoStreamingQuality = VideoQuality.valueOf(streamingQualityString),
                videoDownloadQuality = VideoQuality.valueOf(downloadQualityString)
            )
        }

    override var appConfig: AppConfig
        set(value) {
            val appConfigJson = Gson().toJson(value)
            saveString(APP_CONFIG, appConfigJson)
        }
        get() {
            val appConfigString = getString(APP_CONFIG, getDefaultAppConfig())
            return Gson().fromJson(appConfigString, AppConfig::class.java)
        }

    private fun getDefaultAppConfig() = Gson().toJson(AppConfig())

    override var lastWhatsNewVersion: String
        set(value) {
            saveString(LAST_WHATS_NEW_VERSION, value)
        }
        get() = getString(LAST_WHATS_NEW_VERSION)

    override var lastReviewVersion: InAppReviewPreferences.VersionName
        set(value) {
            val versionNameJson = Gson().toJson(value)
            saveString(LAST_REVIEW_VERSION, versionNameJson)
        }
        get() {
            val versionNameString = getString(LAST_REVIEW_VERSION)
            return Gson().fromJson(
                versionNameString,
                InAppReviewPreferences.VersionName::class.java
            )
                ?: InAppReviewPreferences.VersionName.default
        }

    override var wasPositiveRated: Boolean
        set(value) {
            saveBoolean(APP_WAS_POSITIVE_RATED, value)
        }
        get() = getBoolean(APP_WAS_POSITIVE_RATED)

    override var canResetAppDirectory: Boolean
        set(value) {
            saveBoolean(RESET_APP_DIRECTORY, value)
        }
        get() = getBoolean(RESET_APP_DIRECTORY, true)

    override var isCalendarSyncEnabled: Boolean
        set(value) {
            saveBoolean(IS_CALENDAR_SYNC_ENABLED, value)
        }
        get() = getBoolean(IS_CALENDAR_SYNC_ENABLED, true)

    override var calendarUser: String
        set(value) {
            saveString(CALENDAR_USER, value)
        }
        get() = getString(CALENDAR_USER)

    override var isRelativeDatesEnabled: Boolean
        set(value) {
            saveBoolean(IS_RELATIVE_DATES_ENABLED, value)
        }
        get() = getBoolean(IS_RELATIVE_DATES_ENABLED, true)

    override var isHideInactiveCourses: Boolean
        set(value) {
            saveBoolean(HIDE_INACTIVE_COURSES, value)
        }
        get() = getBoolean(HIDE_INACTIVE_COURSES, true)

    override fun setCalendarSyncEventsDialogShown(courseName: String) {
        saveBoolean(courseName.replaceSpace("_"), true)
    }

    override fun isCalendarSyncEventsDialogShown(courseName: String): Boolean =
        getBoolean(courseName.replaceSpace("_"))

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val PUSH_TOKEN = "push_token"
        private const val EXPIRES_IN = "expires_in"
        private const val USER = "user"
        private const val ACCOUNT = "account"
        private const val LAST_WHATS_NEW_VERSION = "last_whats_new_version"
        private const val LAST_REVIEW_VERSION = "last_review_version"
        private const val APP_WAS_POSITIVE_RATED = "app_was_positive_rated"
        private const val VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY = "video_settings_wifi_download_only"
        private const val VIDEO_SETTINGS_STREAMING_QUALITY = "video_settings_streaming_quality"
        private const val VIDEO_SETTINGS_DOWNLOAD_QUALITY = "video_settings_download_quality"
        private const val APP_CONFIG = "app_config"
        private const val CALENDAR_ID = "CALENDAR_ID"
        private const val RESET_APP_DIRECTORY = "reset_app_directory"
        private const val IS_CALENDAR_SYNC_ENABLED = "IS_CALENDAR_SYNC_ENABLED"
        private const val IS_RELATIVE_DATES_ENABLED = "IS_RELATIVE_DATES_ENABLED"
        private const val HIDE_INACTIVE_COURSES = "HIDE_INACTIVE_COURSES"
        private const val CALENDAR_USER = "CALENDAR_USER"
    }
}
