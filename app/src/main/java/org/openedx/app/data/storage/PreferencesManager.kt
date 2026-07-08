package org.openedx.app.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.CalendarType
import org.openedx.core.domain.model.VideoQuality
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.lmsdirectory.LmsHistoryEntry
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.foundation.extension.replaceSpace
import org.openedx.profile.data.model.Account
import org.openedx.profile.data.storage.ProfilePreferences
import org.openedx.whatsnew.data.storage.WhatsNewPreferences

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "openedx_prefs",
    produceMigrations = { context ->
        listOf(SharedPrefsToDataStoreMigration(context))
    }
)

class PreferencesManager(
    private val context: Context,
    private val appNotifier: AppNotifier,
) :
    CorePreferences,
    ProfilePreferences,
    WhatsNewPreferences,
    InAppReviewPreferences,
    CoursePreferences,
    CalendarPreferences {

    private val dataStore: DataStore<Preferences>
        get() = context.dataStore

    private val encryption = DataStoreEncryption()

    private val gson = Gson()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isKeyInvalidated = false

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val PUSH_TOKEN = stringPreferencesKey("push_token")
        val EXPIRES_IN = longPreferencesKey("expires_in")
        val USER = stringPreferencesKey("user")
        val ACCOUNT = stringPreferencesKey("account")
        val VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY =
            booleanPreferencesKey("video_settings_wifi_download_only")
        val VIDEO_SETTINGS_STREAMING_QUALITY =
            stringPreferencesKey("video_settings_streaming_quality")
        val VIDEO_SETTINGS_DOWNLOAD_QUALITY =
            stringPreferencesKey("video_settings_download_quality")
        val APP_CONFIG = stringPreferencesKey("app_config")
        val CAN_RESET_APP_DIRECTORY = booleanPreferencesKey("reset_app_directory")
        val IS_RELATIVE_DATES_ENABLED = booleanPreferencesKey("is_relative_dates_enabled")
        val CALENDAR_ID = longPreferencesKey("calendar_id")
        val CALENDAR_USER = stringPreferencesKey("calendar_user")
        val CALENDAR_TYPE = stringPreferencesKey("calendar_type")
        val IS_CALENDAR_SYNC_ENABLED = booleanPreferencesKey("is_calendar_sync_enabled")
        val IS_HIDE_INACTIVE_COURSES = booleanPreferencesKey("hide_inactive_courses")
        val LAST_WHATS_NEW_VERSION = stringPreferencesKey("last_whats_new_version")
        val LAST_REVIEW_VERSION = stringPreferencesKey("last_review_version")
        val WAS_POSITIVE_RATED = booleanPreferencesKey("app_was_positive_rated")
        val SELECTED_BASE_URL = stringPreferencesKey("selected_base_url")
        val SELECTED_LMS_ACCENT_COLOR = stringPreferencesKey("selected_lms_accent_color")
        val SELECTED_OAUTH_CLIENT_ID = stringPreferencesKey("selected_oauth_client_id")
        val SELECTED_FEEDBACK_EMAIL = stringPreferencesKey("selected_feedback_email")
        val SELECTED_LMS_LOGO_URL = stringPreferencesKey("selected_lms_logo_url")
        val SELECTED_LMS_LOGIN_BACKGROUND =
            stringPreferencesKey("selected_lms_login_background")
        val SELECTED_LMS_TITLE = stringPreferencesKey("selected_lms_title")
        val LMS_DIRECTORY_CURATED = booleanPreferencesKey("lms_directory_curated")
        val LMS_HISTORY = stringPreferencesKey("lms_history")

        fun calendarSyncDialogShown(courseName: String) =
            booleanPreferencesKey("calendar_sync_dialog_${courseName.replaceSpace("_")}")
    }

    private fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): T =
        runBlocking(Dispatchers.IO) {
            dataStore.data.map { it[key] ?: defaultValue }.first()
        }

    private fun <T> setValue(key: Preferences.Key<T>, value: T) {
        runBlocking(Dispatchers.IO) { dataStore.edit { it[key] = value } }
    }

    private fun getEncryptedString(key: Preferences.Key<String>, defaultValue: String): String {
        val encrypted = if (!isKeyInvalidated) getValue(key, "") else ""
        if (encrypted.isEmpty()) return defaultValue
        val decrypted = encryption.decrypt(encrypted)
        if (decrypted.isEmpty()) {
            // Encrypted data exists but decryption failed — keystore likely invalidated.
            // Clear corrupted data and force re-login.
            isKeyInvalidated = true
            scope.launch {
                clearCorePreferences()
                appNotifier.send(LogoutEvent(true))
            }
        }
        return decrypted.ifEmpty { defaultValue }
    }

    private fun setEncryptedString(key: Preferences.Key<String>, value: String) {
        val encrypted = if (value.isEmpty()) "" else encryption.encrypt(value)
        setValue(key, encrypted)
    }

    override suspend fun clearCorePreferences() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.PUSH_TOKEN)
            prefs.remove(Keys.EXPIRES_IN)
            prefs.remove(Keys.USER)
            prefs.remove(Keys.ACCOUNT)
            // LMS Directory: drop the selected platform on logout so the app returns to
            // the platform picker (matches iOS) instead of silently reusing the previous
            // LMS's host/branding for the next user. No-op for single-tenant builds.
            prefs.remove(Keys.SELECTED_BASE_URL)
            prefs.remove(Keys.SELECTED_LMS_ACCENT_COLOR)
            prefs.remove(Keys.SELECTED_OAUTH_CLIENT_ID)
            prefs.remove(Keys.SELECTED_FEEDBACK_EMAIL)
            prefs.remove(Keys.SELECTED_LMS_LOGO_URL)
            prefs.remove(Keys.SELECTED_LMS_LOGIN_BACKGROUND)
            prefs.remove(Keys.SELECTED_LMS_TITLE)
        }
    }

    override suspend fun clearCalendarPreferences() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.CALENDAR_ID)
            prefs.remove(Keys.CALENDAR_TYPE)
            prefs.remove(Keys.IS_CALENDAR_SYNC_ENABLED)
            prefs.remove(Keys.IS_HIDE_INACTIVE_COURSES)
        }
    }

    override var accessToken: String
        get() = getEncryptedString(Keys.ACCESS_TOKEN, "")
        set(value) = setEncryptedString(Keys.ACCESS_TOKEN, value)

    override var refreshToken: String
        get() = getEncryptedString(Keys.REFRESH_TOKEN, "")
        set(value) = setEncryptedString(Keys.REFRESH_TOKEN, value)

    override var pushToken: String
        get() = getEncryptedString(Keys.PUSH_TOKEN, "")
        set(value) = setEncryptedString(Keys.PUSH_TOKEN, value)

    override var accessTokenExpiresAt: Long
        get() = getValue(Keys.EXPIRES_IN, 0L)
        set(value) = setValue(Keys.EXPIRES_IN, value)

    override var user: User?
        get() {
            val json = getEncryptedString(Keys.USER, "")
            return if (json.isEmpty()) null else Gson().fromJson(json, User::class.java)
        }
        set(value) = setEncryptedString(Keys.USER, Gson().toJson(value))

    override var videoSettings: VideoSettings
        get() {
            val wifiDownloadOnly = getValue(Keys.VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY, true)
            val streamingQuality =
                getValue(Keys.VIDEO_SETTINGS_STREAMING_QUALITY, VideoQuality.AUTO.name)
            val downloadQuality =
                getValue(Keys.VIDEO_SETTINGS_DOWNLOAD_QUALITY, VideoQuality.AUTO.name)
            return VideoSettings(
                wifiDownloadOnly = wifiDownloadOnly,
                videoStreamingQuality = VideoQuality.valueOf(streamingQuality),
                videoDownloadQuality = VideoQuality.valueOf(downloadQuality)
            )
        }
        set(value) {
            runBlocking(Dispatchers.IO) {
                dataStore.edit { prefs ->
                    prefs[Keys.VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY] = value.wifiDownloadOnly
                    prefs[Keys.VIDEO_SETTINGS_STREAMING_QUALITY] = value.videoStreamingQuality.name
                    prefs[Keys.VIDEO_SETTINGS_DOWNLOAD_QUALITY] = value.videoDownloadQuality.name
                }
            }
        }

    override var appConfig: AppConfig
        get() {
            val json = getValue(Keys.APP_CONFIG, Gson().toJson(AppConfig()))
            return Gson().fromJson(json, AppConfig::class.java)
        }
        set(value) = setValue(Keys.APP_CONFIG, Gson().toJson(value))

    override var canResetAppDirectory: Boolean
        get() = getValue(Keys.CAN_RESET_APP_DIRECTORY, true)
        set(value) = setValue(Keys.CAN_RESET_APP_DIRECTORY, value)

    override var isRelativeDatesEnabled: Boolean
        get() = getValue(Keys.IS_RELATIVE_DATES_ENABLED, true)
        set(value) = setValue(Keys.IS_RELATIVE_DATES_ENABLED, value)

    override var selectedBaseUrl: String?
        get() = getValue(Keys.SELECTED_BASE_URL, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_BASE_URL, value.orEmpty())

    override var selectedLmsAccentColor: String?
        get() = getValue(Keys.SELECTED_LMS_ACCENT_COLOR, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_LMS_ACCENT_COLOR, value.orEmpty())

    override var selectedOAuthClientId: String?
        get() = getValue(Keys.SELECTED_OAUTH_CLIENT_ID, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_OAUTH_CLIENT_ID, value.orEmpty())

    override var selectedFeedbackEmail: String?
        get() = getValue(Keys.SELECTED_FEEDBACK_EMAIL, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_FEEDBACK_EMAIL, value.orEmpty())

    override var selectedLmsLogoUrl: String?
        get() = getValue(Keys.SELECTED_LMS_LOGO_URL, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_LMS_LOGO_URL, value.orEmpty())

    override var selectedLmsLoginBackgroundUrl: String?
        get() = getValue(Keys.SELECTED_LMS_LOGIN_BACKGROUND, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_LMS_LOGIN_BACKGROUND, value.orEmpty())

    override var selectedLmsTitle: String?
        get() = getValue(Keys.SELECTED_LMS_TITLE, "").ifEmpty { null }
        set(value) = setValue(Keys.SELECTED_LMS_TITLE, value.orEmpty())

    override var lmsDirectoryCurated: Boolean
        get() = getValue(Keys.LMS_DIRECTORY_CURATED, false)
        set(value) = setValue(Keys.LMS_DIRECTORY_CURATED, value)

    override var lmsHistory: List<LmsHistoryEntry>
        get() {
            val json = getValue(Keys.LMS_HISTORY, "")
            if (json.isEmpty()) return emptyList()
            return runCatching {
                val type = object : TypeToken<List<LmsHistoryEntry>>() {}.type
                gson.fromJson<List<LmsHistoryEntry>>(json, type) ?: emptyList()
            }.getOrDefault(emptyList())
        }
        set(value) = setValue(Keys.LMS_HISTORY, gson.toJson(value))

    override var profile: Account?
        get() {
            val json = getEncryptedString(Keys.ACCOUNT, "")
            return if (json.isEmpty()) null else Gson().fromJson(json, Account::class.java)
        }
        set(value) = setEncryptedString(Keys.ACCOUNT, Gson().toJson(value))

    override var lastWhatsNewVersion: String
        get() = getValue(Keys.LAST_WHATS_NEW_VERSION, "")
        set(value) = setValue(Keys.LAST_WHATS_NEW_VERSION, value)

    override var lastReviewVersion: InAppReviewPreferences.VersionName
        get() {
            val json = getValue(Keys.LAST_REVIEW_VERSION, "")
            return if (json.isEmpty()) {
                InAppReviewPreferences.VersionName.default
            } else {
                Gson().fromJson(json, InAppReviewPreferences.VersionName::class.java)
                    ?: InAppReviewPreferences.VersionName.default
            }
        }
        set(value) = setValue(Keys.LAST_REVIEW_VERSION, Gson().toJson(value))

    override var wasPositiveRated: Boolean
        get() = getValue(Keys.WAS_POSITIVE_RATED, false)
        set(value) = setValue(Keys.WAS_POSITIVE_RATED, value)

    override var calendarId: Long
        get() = getValue(Keys.CALENDAR_ID, CalendarManager.CALENDAR_DOES_NOT_EXIST)
        set(value) = setValue(Keys.CALENDAR_ID, value)

    override var calendarUser: String
        get() = getValue(Keys.CALENDAR_USER, "")
        set(value) = setValue(Keys.CALENDAR_USER, value)

    override var calendarType: CalendarType
        get() {
            val stored = getValue(Keys.CALENDAR_TYPE, CalendarType.LOCAL.name)
            return runCatching { CalendarType.valueOf(stored) }.getOrDefault(CalendarType.LOCAL)
        }
        set(value) = setValue(Keys.CALENDAR_TYPE, value.name)

    override var isCalendarSyncEnabled: Boolean
        get() = getValue(Keys.IS_CALENDAR_SYNC_ENABLED, true)
        set(value) = setValue(Keys.IS_CALENDAR_SYNC_ENABLED, value)

    override var isHideInactiveCourses: Boolean
        get() = getValue(Keys.IS_HIDE_INACTIVE_COURSES, true)
        set(value) = setValue(Keys.IS_HIDE_INACTIVE_COURSES, value)

    override fun setCalendarSyncEventsDialogShown(courseName: String) {
        setValue(Keys.calendarSyncDialogShown(courseName), true)
    }

    override fun isCalendarSyncEventsDialogShown(courseName: String): Boolean {
        return getValue(Keys.calendarSyncDialogShown(courseName), false)
    }
}
