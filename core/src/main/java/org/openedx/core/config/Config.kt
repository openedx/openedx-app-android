package org.openedx.core.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.openedx.core.domain.model.AgreementUrls
import java.io.InputStreamReader

@Suppress("TooManyFunctions")
class Config(context: Context) {

    private var configProperties: JsonObject = try {
        val inputStream = context.assets.open("config/config.json")
        val config = JsonParser.parseReader(InputStreamReader(inputStream))
        config.asJsonObject
    } catch (e: Exception) {
        e.printStackTrace()
        JsonObject()
    }

    fun getAppId(): String {
        return getString(APPLICATION_ID, "")
    }

    fun getApiHostURL(): String {
        return getString(API_HOST_URL)
    }

    fun getUriScheme(): String {
        return getString(URI_SCHEME)
    }

    fun getOAuthClientId(): String {
        return getString(OAUTH_CLIENT_ID)
    }

    fun getAccessTokenType(): String {
        return getString(TOKEN_TYPE)
    }

    fun getFaqUrl(): String {
        return getString(FAQ_URL)
    }

    fun getFeedbackEmailAddress(): String {
        return getString(FEEDBACK_EMAIL_ADDRESS)
    }

    fun getPlatformName(): String {
        return getString(PLATFORM_NAME)
    }

    fun getAgreement(locale: String): AgreementUrls {
        val agreement =
            getObjectOrNewInstance(AGREEMENT_URLS, AgreementUrlsConfig::class.java).mapToDomain()
        return agreement.getAgreementForLocale(locale)
    }

    fun getFirebaseConfig(): FirebaseConfig {
        return getObjectOrNewInstance(FIREBASE, FirebaseConfig::class.java)
    }

    fun getBrazeConfig(): BrazeConfig {
        return getObjectOrNewInstance(BRAZE, BrazeConfig::class.java)
    }

    fun getFacebookConfig(): FacebookConfig {
        return getObjectOrNewInstance(FACEBOOK, FacebookConfig::class.java)
    }

    fun getGoogleConfig(): GoogleConfig {
        return getObjectOrNewInstance(GOOGLE, GoogleConfig::class.java)
    }

    fun getMicrosoftConfig(): MicrosoftConfig {
        return getObjectOrNewInstance(MICROSOFT, MicrosoftConfig::class.java)
    }

    fun isSocialAuthEnabled() = getBoolean(SOCIAL_AUTH_ENABLED, false)

    fun getDiscoveryConfig(): DiscoveryConfig {
        return getObjectOrNewInstance(DISCOVERY, DiscoveryConfig::class.java)
    }

    fun getProgramConfig(): ProgramConfig {
        return getObjectOrNewInstance(PROGRAM, ProgramConfig::class.java)
    }

    fun getDashboardConfig(): DashboardConfig {
        return getObjectOrNewInstance(DASHBOARD, DashboardConfig::class.java)
    }

    fun getBranchConfig(): BranchConfig {
        return getObjectOrNewInstance(BRANCH, BranchConfig::class.java)
    }

    fun isWhatsNewEnabled(): Boolean {
        return getBoolean(WHATS_NEW_ENABLED, false)
    }

    fun isPreLoginExperienceEnabled(): Boolean {
        return getBoolean(PRE_LOGIN_EXPERIENCE_ENABLED, true)
    }

    fun getCourseUIConfig(): UIConfig {
        return getObjectOrNewInstance(UI_COMPONENTS, UIConfig::class.java)
    }

    fun isRegistrationEnabled(): Boolean {
        return getBoolean(REGISTRATION_ENABLED, true)
    }

    fun isBrowserLoginEnabled(): Boolean {
        return getBoolean(BROWSER_LOGIN, false)
    }

    fun isBrowserRegistrationEnabled(): Boolean {
        return getBoolean(BROWSER_REGISTRATION, false)
    }

    private fun getString(key: String, defaultValue: String = ""): String {
        val element = getObject(key)
        return if (element != null) {
            element.asString
        } else {
            defaultValue
        }
    }

    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val element = getObject(key)
        return element?.asBoolean ?: defaultValue
    }

    private fun <T> getObjectOrNewInstance(key: String, cls: Class<T>): T {
        val element = getObject(key)
        return if (element != null) {
            val gson = Gson()
            gson.fromJson(element, cls)
        } else {
            try {
                cls.getDeclaredConstructor().newInstance()
            } catch (e: InstantiationException) {
                throw ConfigParsingException(e)
            } catch (e: IllegalAccessException) {
                throw ConfigParsingException(e)
            }
        }
    }

    class ConfigParsingException(cause: Throwable) : Exception(cause)

    private fun getObject(key: String): JsonElement? {
        return configProperties.get(key)
    }

    companion object {
        private const val APPLICATION_ID = "APPLICATION_ID"
        private const val API_HOST_URL = "API_HOST_URL"
        private const val URI_SCHEME = "URI_SCHEME"
        private const val OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID"
        private const val TOKEN_TYPE = "TOKEN_TYPE"
        private const val FAQ_URL = "FAQ_URL"
        private const val FEEDBACK_EMAIL_ADDRESS = "FEEDBACK_EMAIL_ADDRESS"
        private const val AGREEMENT_URLS = "AGREEMENT_URLS"
        private const val WHATS_NEW_ENABLED = "WHATS_NEW_ENABLED"
        private const val SOCIAL_AUTH_ENABLED = "SOCIAL_AUTH_ENABLED"
        private const val FIREBASE = "FIREBASE"
        private const val BRAZE = "BRAZE"
        private const val FACEBOOK = "FACEBOOK"
        private const val GOOGLE = "GOOGLE"
        private const val MICROSOFT = "MICROSOFT"
        private const val PRE_LOGIN_EXPERIENCE_ENABLED = "PRE_LOGIN_EXPERIENCE_ENABLED"
        private const val REGISTRATION_ENABLED = "REGISTRATION_ENABLED"
        private const val BROWSER_LOGIN = "BROWSER_LOGIN"
        private const val BROWSER_REGISTRATION = "BROWSER_REGISTRATION"
        private const val DISCOVERY = "DISCOVERY"
        private const val PROGRAM = "PROGRAM"
        private const val DASHBOARD = "DASHBOARD"
        private const val BRANCH = "BRANCH"
        private const val UI_COMPONENTS = "UI_COMPONENTS"
        private const val PLATFORM_NAME = "PLATFORM_NAME"
    }

    enum class ViewType {
        NATIVE,
        WEBVIEW
    }
}
