package org.openedx.core.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.openedx.core.R
import java.io.InputStreamReader

class Config(context: Context) {

    private var configProperties: JsonObject

    init {
        configProperties = try {
            val inputStream = context.resources.openRawResource(R.raw.config)
            val parser = JsonParser()
            val config = parser.parse(InputStreamReader(inputStream))
            config.asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
    }

    fun getApiHostURL(): String {
        return getString(API_HOST_URL, "")
    }

    fun getOAuthClientId(): String {
        return getString(OAUTH_CLIENT_ID, "")
    }

    fun getAccessTokenType(): String {
        return getString(TOKEN_TYPE, "")
    }

    fun getFeedbackEmailAddress(): String {
        return getString(FEEDBACK_EMAIL_ADDRESS, "")
    }

    fun getAgreementUrlsConfig(): AgreementUrlsConfig {
        return getObjectOrNewInstance(AGREEMENT_URLS, AgreementUrlsConfig::class.java)
    }

    fun getFirebaseConfig(): FirebaseConfig {
        return getObjectOrNewInstance(FIREBASE, FirebaseConfig::class.java)
    }

    fun isWhatsNewEnabled(): Boolean {
        return getBoolean(WHATS_NEW_ENABLED, false)
    }

    private fun getString(key: String, defaultValue: String): String {
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
                cls.newInstance()
            } catch (e: InstantiationException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun getObject(key: String): JsonElement? {
        return configProperties.get(key)
    }

    companion object {
        private const val API_HOST_URL = "API_HOST_URL"
        private const val OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID"
        private const val TOKEN_TYPE = "TOKEN_TYPE"
        private const val FEEDBACK_EMAIL_ADDRESS = "FEEDBACK_EMAIL_ADDRESS"
        private const val AGREEMENT_URLS = "AGREEMENT_URLS"
        private const val WHATS_NEW_ENABLED = "WHATS_NEW_ENABLED"
        private const val FIREBASE = "FIREBASE"
    }
}
