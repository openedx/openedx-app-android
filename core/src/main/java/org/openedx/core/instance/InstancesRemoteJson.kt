package org.openedx.core.instance

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.util.Locale

/**
 * Remote catalogs are sent in snake_case. These are the keys renamed to the bundled schema's
 * UPPER_SNAKE_CASE before parsing; every other key passes through unchanged, so a remote field
 * outside this map has to be sent in UPPER_SNAKE_CASE already. Shared with iOS.
 */
internal val remoteToConfigKeyMap: Map<String, String> = mapOf(
    "key" to "KEY",
    "name" to "NAME",
    "color" to "COLOR",
    "instance_name" to "INSTANCE_NAME",
    "oauth_client_id" to "OAUTH_CLIENT_ID",
    "api_host_url" to "API_HOST_URL",
    "sso_url" to "SSO_URL",
    "sso_url_successful_login" to "SSO_URL_SUCCESSFUL_LOGIN",
    "environment_display_name" to "ENVIRONMENT_DISPLAY_NAME",
    "is_switch_instance_login_enabled" to "IS_SWITCH_INSTANCE_LOGIN_ENABLED",
    "ui_components" to "UI_COMPONENTS",
    "course_dropdown_navigation_enabled" to "COURSE_DROPDOWN_NAVIGATION_ENABLED",
    "course_unit_progress_enabled" to "COURSE_UNIT_PROGRESS_ENABLED",
    "login_registration_enabled" to "LOGIN_REGISTRATION_ENABLED",
    "saml_sso_login_enabled" to "SAML_SSO_LOGIN_ENABLED",
    "saml_sso_default_login_button" to "SAML_SSO_DEFAULT_LOGIN_BUTTON",
    "theme" to "THEME",
    "light" to "LIGHT",
    "dark" to "DARK",
    "logo_url" to "LOGO_URL",
    "header_background_url" to "HEADER_BACKGROUND_URL",
)

/** Renames mapped keys at every nesting depth; unmapped keys and all values stay as they are. */
internal fun JsonObject.normalizedRemoteKeys(): JsonObject {
    val result = JsonObject()
    entrySet().forEach { (key, value) ->
        val mappedKey = remoteToConfigKeyMap[key.lowercase(Locale.ROOT)] ?: key
        result.add(mappedKey, if (value.isJsonObject) value.asJsonObject.normalizedRemoteKeys() else value)
    }
    return result
}

/**
 * Parses a remote catalog: a bare array, or an object with the array under `instances` or
 * `INSTANCES`. Keys are normalized, then entries go through the same rules as the bundled
 * catalog.
 */
fun InstancesConfig.Companion.fromRemoteJson(
    json: String,
    languageCode: String = Locale.getDefault().language,
): InstancesConfig {
    val root: JsonElement = try {
        JsonParser.parseString(json)
    } catch (e: JsonParseException) {
        throw InstancesJsonException("Catalog is not valid JSON", e)
    }
    val array = root.instancesArray() ?: throw InstancesJsonException("Catalog has no instances array")
    val normalized = JsonArray().apply {
        array.forEach { add(if (it.isJsonObject) it.asJsonObject.normalizedRemoteKeys() else it) }
    }
    return fromJson(normalized, languageCode = languageCode)
}

private fun JsonElement.instancesArray(): JsonArray? = when {
    isJsonArray -> asJsonArray
    isJsonObject -> (asJsonObject.get(InstancesConfig.INSTANCES) ?: asJsonObject.get("instances"))
        ?.takeIf { it.isJsonArray }
        ?.asJsonArray

    else -> null
}
