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
import org.openedx.core.instance.StorageNamespace
import org.openedx.core.instance.StorageNamespaceProvider
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
    private val namespaces: StorageNamespaceProvider,
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isKeyInvalidated = false

    /**
     * Keys tied to a learner on one site: they are namespaced per instance, so each site keeps
     * its own session, profile and calendar state. Everything in [Keys] is device-wide.
     */
    @Suppress("TooManyFunctions")
    private object InstanceKeys {
        fun accessToken(ns: StorageNamespace) = stringPreferencesKey(ns.key("access_token"))
        fun refreshToken(ns: StorageNamespace) = stringPreferencesKey(ns.key("refresh_token"))
        fun pushToken(ns: StorageNamespace) = stringPreferencesKey(ns.key("push_token"))
        fun expiresIn(ns: StorageNamespace) = longPreferencesKey(ns.key("expires_in"))
        fun user(ns: StorageNamespace) = stringPreferencesKey(ns.key("user"))
        fun account(ns: StorageNamespace) = stringPreferencesKey(ns.key("account"))
        fun appConfig(ns: StorageNamespace) = stringPreferencesKey(ns.key("app_config"))
        fun lastSignedInUsername(ns: StorageNamespace) = stringPreferencesKey(ns.key("last_signed_in_username"))
        fun isRelativeDatesEnabled(ns: StorageNamespace) =
            booleanPreferencesKey(ns.key("is_relative_dates_enabled"))
        fun calendarId(ns: StorageNamespace) = longPreferencesKey(ns.key("calendar_id"))
        fun calendarUser(ns: StorageNamespace) = stringPreferencesKey(ns.key("calendar_user"))
        fun calendarType(ns: StorageNamespace) = stringPreferencesKey(ns.key("calendar_type"))
        fun isCalendarSyncEnabled(ns: StorageNamespace) =
            booleanPreferencesKey(ns.key("is_calendar_sync_enabled"))
        fun isHideInactiveCourses(ns: StorageNamespace) =
            booleanPreferencesKey(ns.key("hide_inactive_courses"))
        fun calendarSyncDialogShown(ns: StorageNamespace, courseName: String) =
            booleanPreferencesKey(ns.key("calendar_sync_dialog_${courseName.replaceSpace("_")}"))
    }

    private val ns: StorageNamespace
        get() = namespaces.current

    private object Keys {
        val VIDEO_SETTINGS_WIFI_DOWNLOAD_ONLY =
            booleanPreferencesKey("video_settings_wifi_download_only")
        val VIDEO_SETTINGS_STREAMING_QUALITY =
            stringPreferencesKey("video_settings_streaming_quality")
        val VIDEO_SETTINGS_DOWNLOAD_QUALITY =
            stringPreferencesKey("video_settings_download_quality")
        val CAN_RESET_APP_DIRECTORY = booleanPreferencesKey("reset_app_directory")
        val LAST_WHATS_NEW_VERSION = stringPreferencesKey("last_whats_new_version")
        val LAST_REVIEW_VERSION = stringPreferencesKey("last_review_version")
        val WAS_POSITIVE_RATED = booleanPreferencesKey("app_was_positive_rated")
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
        clearSession(namespaces.current)
    }

    override fun hasSession(instanceKey: String?): Boolean {
        val namespace = namespaces.namespaceFor(instanceKey)
        return getValue(InstanceKeys.accessToken(namespace), "").isNotEmpty() &&
            getValue(InstanceKeys.user(namespace), "").isNotEmpty()
    }

    override suspend fun clearSession(instanceKey: String?) {
        clearSession(namespaces.namespaceFor(instanceKey))
    }

    private suspend fun clearSession(namespace: StorageNamespace) {
        dataStore.edit { prefs ->
            prefs.remove(InstanceKeys.accessToken(namespace))
            prefs.remove(InstanceKeys.refreshToken(namespace))
            prefs.remove(InstanceKeys.pushToken(namespace))
            prefs.remove(InstanceKeys.expiresIn(namespace))
            prefs.remove(InstanceKeys.user(namespace))
            prefs.remove(InstanceKeys.account(namespace))
        }
    }

    override fun accessToken(instanceKey: String?): String {
        return getEncryptedString(InstanceKeys.accessToken(namespaces.namespaceFor(instanceKey)), "")
    }

    override fun refreshToken(instanceKey: String?): String {
        return getEncryptedString(InstanceKeys.refreshToken(namespaces.namespaceFor(instanceKey)), "")
    }

    override fun saveTokens(instanceKey: String?, accessToken: String, refreshToken: String, expiresAt: Long) {
        val namespace = namespaces.namespaceFor(instanceKey)
        setEncryptedString(InstanceKeys.accessToken(namespace), accessToken)
        setEncryptedString(InstanceKeys.refreshToken(namespace), refreshToken)
        setValue(InstanceKeys.expiresIn(namespace), expiresAt)
    }

    override suspend fun clearCalendarPreferences() {
        val namespace = ns
        dataStore.edit { prefs ->
            prefs.remove(InstanceKeys.calendarId(namespace))
            prefs.remove(InstanceKeys.calendarType(namespace))
            prefs.remove(InstanceKeys.isCalendarSyncEnabled(namespace))
            prefs.remove(InstanceKeys.isHideInactiveCourses(namespace))
        }
    }

    override var accessToken: String
        get() = getEncryptedString(InstanceKeys.accessToken(ns), "")
        set(value) = setEncryptedString(InstanceKeys.accessToken(ns), value)

    override var refreshToken: String
        get() = getEncryptedString(InstanceKeys.refreshToken(ns), "")
        set(value) = setEncryptedString(InstanceKeys.refreshToken(ns), value)

    override var pushToken: String
        get() = getEncryptedString(InstanceKeys.pushToken(ns), "")
        set(value) = setEncryptedString(InstanceKeys.pushToken(ns), value)

    override var accessTokenExpiresAt: Long
        get() = getValue(InstanceKeys.expiresIn(ns), 0L)
        set(value) = setValue(InstanceKeys.expiresIn(ns), value)

    override var user: User?
        get() {
            val json = getEncryptedString(InstanceKeys.user(ns), "")
            return if (json.isEmpty()) null else Gson().fromJson(json, User::class.java)
        }
        set(value) = setEncryptedString(InstanceKeys.user(ns), Gson().toJson(value))

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
            val json = getValue(InstanceKeys.appConfig(ns), Gson().toJson(AppConfig()))
            return Gson().fromJson(json, AppConfig::class.java)
        }
        set(value) = setValue(InstanceKeys.appConfig(ns), Gson().toJson(value))

    override var canResetAppDirectory: Boolean
        get() = getValue(Keys.CAN_RESET_APP_DIRECTORY, true)
        set(value) = setValue(Keys.CAN_RESET_APP_DIRECTORY, value)

    override var isRelativeDatesEnabled: Boolean
        get() = getValue(InstanceKeys.isRelativeDatesEnabled(ns), true)
        set(value) = setValue(InstanceKeys.isRelativeDatesEnabled(ns), value)

    override var lastSignedInUsername: String
        get() = getValue(InstanceKeys.lastSignedInUsername(ns), "")
        set(value) = setValue(InstanceKeys.lastSignedInUsername(ns), value)

    override var profile: Account?
        get() {
            val json = getEncryptedString(InstanceKeys.account(ns), "")
            return if (json.isEmpty()) null else Gson().fromJson(json, Account::class.java)
        }
        set(value) = setEncryptedString(InstanceKeys.account(ns), Gson().toJson(value))

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
        get() = getValue(InstanceKeys.calendarId(ns), CalendarManager.CALENDAR_DOES_NOT_EXIST)
        set(value) = setValue(InstanceKeys.calendarId(ns), value)

    override var calendarUser: String
        get() = getValue(InstanceKeys.calendarUser(ns), "")
        set(value) = setValue(InstanceKeys.calendarUser(ns), value)

    override var calendarType: CalendarType
        get() {
            val stored = getValue(InstanceKeys.calendarType(ns), CalendarType.LOCAL.name)
            return runCatching { CalendarType.valueOf(stored) }.getOrDefault(CalendarType.LOCAL)
        }
        set(value) = setValue(InstanceKeys.calendarType(ns), value.name)

    override var isCalendarSyncEnabled: Boolean
        get() = getValue(InstanceKeys.isCalendarSyncEnabled(ns), true)
        set(value) = setValue(InstanceKeys.isCalendarSyncEnabled(ns), value)

    override var isHideInactiveCourses: Boolean
        get() = getValue(InstanceKeys.isHideInactiveCourses(ns), true)
        set(value) = setValue(InstanceKeys.isHideInactiveCourses(ns), value)

    override fun setCalendarSyncEventsDialogShown(courseName: String) {
        setValue(InstanceKeys.calendarSyncDialogShown(ns, courseName), true)
    }

    override fun isCalendarSyncEventsDialogShown(courseName: String): Boolean {
        return getValue(InstanceKeys.calendarSyncDialogShown(ns, courseName), false)
    }
}
