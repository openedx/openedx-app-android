package org.openedx.core.instance

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.net.URI
import java.text.Normalizer
import java.util.Locale

/**
 * One Open edX platform the app can sign in to: its own host, OAuth client, branding and
 * per-platform overrides of the app-level configuration.
 *
 * Instances are identified by their stable [key], never by their (possibly localized)
 * display name or their position in the catalog.
 */
data class Instance(
    /** Machine-readable identifier, e.g. "niepd". Falls back to a slugified [name]. */
    val key: String,
    val name: String,
    /** Localized display name; falls back to the English entry, then to [name]. */
    val instanceName: String,
    val color: String,
    val oAuthClientId: String,
    val apiHostUrl: String,
    val ssoUrl: String?,
    val ssoFinishedUrl: String?,
    val environmentDisplayName: String?,
    val isSwitchInstanceLoginEnabled: Boolean,
    /** Remote URL, or the name of a bundled drawable. `null` keeps the default logo. */
    val logoUrl: String?,
    /** Remote URL, or the name of a bundled drawable. `null` keeps the default header. */
    val headerBackgroundUrl: String?,
    /** `null` derives the palette from [color] instead. */
    val themeColors: InstanceThemeColors?,
    /** Defaults to [instanceName] when unset, so it is never blank. */
    val platformName: String,
    val isRoundedCorners: Boolean,
    val buttonCornersRadius: Double,
    /**
     * The entry exactly as configured, in UPPER_SNAKE_CASE. Instance-level overrides of
     * app-level keys (`AGREEMENT_URLS`, `DISCOVERY`, `UI_COMPONENTS`, ...) are read from here.
     */
    val config: JsonObject,
) {

    companion object {
        const val KEY = "KEY"
        const val NAME = "NAME"
        const val INSTANCE_NAME = "INSTANCE_NAME"
        const val COLOR = "COLOR"
        const val OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID"
        const val API_HOST_URL = "API_HOST_URL"
        const val SSO_URL = "SSO_URL"
        const val SSO_FINISHED_URL = "SSO_FINISHED_URL"
        const val SSO_URL_SUCCESSFUL_LOGIN = "SSO_URL_SUCCESSFUL_LOGIN"
        const val ENVIRONMENT_DISPLAY_NAME = "ENVIRONMENT_DISPLAY_NAME"
        const val IS_SWITCH_INSTANCE_LOGIN_ENABLED = "IS_SWITCH_INSTANCE_LOGIN_ENABLED"
        const val PLATFORM_NAME = "PLATFORM_NAME"
        const val THEME = "THEME"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_LOGO_URL = "LOGO_URL"
        const val THEME_HEADER_BACKGROUND_URL = "HEADER_BACKGROUND_URL"
        const val THEME_ROUNDED_CORNERS_STYLE = "ROUNDED_CORNERS_STYLE"
        const val THEME_BUTTON_CORNERS_RADIUS = "BUTTON_CORNERS_RADIUS"

        private const val DEFAULT_COLOR = "#007AFF"
        private const val DEFAULT_BUTTON_CORNERS_RADIUS = 8.0
        private const val DEFAULT_LANGUAGE = "en"

        /**
         * Parses one `INSTANCES` entry. Returns `null` when a required field (`NAME`,
         * `API_HOST_URL`, `OAUTH_CLIENT_ID`) is missing or `API_HOST_URL` isn't an absolute
         * URL, so a malformed entry is dropped instead of producing a half-populated instance.
         */
        @Suppress("ReturnCount")
        fun fromJson(
            json: JsonObject,
            languageCode: String = Locale.getDefault().language,
        ): Instance? {
            val name = json.string(NAME)?.takeIf { it.isNotBlank() } ?: return null
            val apiHostUrl = json.string(API_HOST_URL)?.takeIf { it.isAbsoluteUrl() } ?: return null
            val oAuthClientId = json.string(OAUTH_CLIENT_ID)?.takeIf { it.isNotBlank() } ?: return null

            val key = json.string(KEY)?.takeIf { it.isNotBlank() } ?: slugify(name)
            val localizedNames = json.stringMap(INSTANCE_NAME)
            val instanceName = localizedNames[languageCode]
                ?: localizedNames[DEFAULT_LANGUAGE]
                ?: name

            // Logo and header live inside THEME, as siblings of LIGHT/DARK. The palette maps'
            // own field names (accent_color, ...) stay lowercase: they are the theme engine's
            // contract, shared with iOS, not part of this schema.
            val theme = json.obj(THEME)
            val themeColors = theme?.let {
                InstanceThemeColors(
                    light = it.stringMap(THEME_LIGHT),
                    dark = it.stringMap(THEME_DARK),
                )
            }

            return Instance(
                key = key,
                name = name,
                instanceName = instanceName,
                color = json.nonBlankString(COLOR) ?: DEFAULT_COLOR,
                oAuthClientId = oAuthClientId,
                apiHostUrl = apiHostUrl,
                ssoUrl = json.nonBlankString(SSO_URL),
                ssoFinishedUrl = json.nonBlankString(SSO_FINISHED_URL)
                    ?: json.nonBlankString(SSO_URL_SUCCESSFUL_LOGIN),
                environmentDisplayName = json.nonBlankString(ENVIRONMENT_DISPLAY_NAME),
                isSwitchInstanceLoginEnabled = json.boolean(IS_SWITCH_INSTANCE_LOGIN_ENABLED) ?: false,
                logoUrl = theme?.nonBlankString(THEME_LOGO_URL),
                headerBackgroundUrl = theme?.nonBlankString(THEME_HEADER_BACKGROUND_URL),
                themeColors = themeColors,
                platformName = json.nonBlankString(PLATFORM_NAME) ?: instanceName,
                // Absent or non-boolean means rounded, same as the app-level theme default.
                isRoundedCorners = theme?.boolean(THEME_ROUNDED_CORNERS_STYLE) != false,
                buttonCornersRadius = theme?.double(THEME_BUTTON_CORNERS_RADIUS)
                    ?: DEFAULT_BUTTON_CORNERS_RADIUS,
                config = json.deepCopy(),
            )
        }

        internal fun slugify(name: String): String {
            return Normalizer.normalize(name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .replace("[^a-z0-9]+".toRegex(), "-")
                .trim('-')
        }

        private fun String.isAbsoluteUrl(): Boolean {
            return try {
                val uri = URI(this)
                !uri.scheme.isNullOrEmpty() && !uri.host.isNullOrEmpty()
            } catch (_: Exception) {
                false
            }
        }
    }
}

/**
 * Per-mode color overrides from an instance's `THEME` block. Keys are the shared palette
 * field names (`accent_color`, `text_primary`, ...); values are hex strings.
 */
data class InstanceThemeColors(
    val light: Map<String, String>,
    val dark: Map<String, String>,
)

internal fun JsonObject.primitive(key: String): JsonElement? {
    val element = get(key) ?: return null
    return if (element.isJsonPrimitive) element else null
}

internal fun JsonObject.string(key: String): String? {
    val element = primitive(key) ?: return null
    return if (element.asJsonPrimitive.isString) element.asString else null
}

internal fun JsonObject.nonBlankString(key: String): String? = string(key)?.takeIf { it.isNotBlank() }

internal fun JsonObject.boolean(key: String): Boolean? {
    val element = primitive(key) ?: return null
    return if (element.asJsonPrimitive.isBoolean) element.asBoolean else null
}

internal fun JsonObject.double(key: String): Double? {
    val element = primitive(key) ?: return null
    return if (element.asJsonPrimitive.isNumber) element.asDouble else null
}

internal fun JsonObject.obj(key: String): JsonObject? {
    val element = get(key) ?: return null
    return if (element.isJsonObject) element.asJsonObject else null
}

internal fun JsonObject.stringMap(key: String): Map<String, String> {
    val obj = obj(key) ?: return emptyMap()
    return obj.entrySet()
        .filter { (_, value) -> value.isJsonPrimitive && value.asJsonPrimitive.isString }
        .associate { (entryKey, value) -> entryKey to value.asString }
}
