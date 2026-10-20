package org.openedx.core.config

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider

/**
 * The [Config] the app runs with: the selected instance layered over the app-level config.
 *
 * For the keys an instance may override, a value the instance sets wins; anything it leaves
 * out, and every key while no instance is selected, comes from the app-level config. Keys
 * outside [INSTANCE_KEYS] (Firebase, Braze, Branch, social sign-in providers, URI scheme,
 * feature flags such as What's New) always stay app-level.
 */
open class InstanceAwareConfig(
    appConfig: JsonObject,
    private val instanceProvider: InstanceProvider,
) : Config(appConfig) {

    constructor(context: Context, instanceProvider: InstanceProvider) :
        this(Config.loadBundled(context), instanceProvider)

    override fun getObject(key: String): JsonElement? {
        val instance = instanceProvider.currentInstance
        return instance?.let { instanceValue(it, key) } ?: super.getObject(key)
    }

    private fun instanceValue(instance: Instance, key: String): JsonElement? {
        val config = instance.config
        return when (key) {
            // Resolved through the model, which validates them and applies its defaults.
            API_HOST_URL -> JsonPrimitive(instance.apiHostUrl)
            OAUTH_CLIENT_ID -> JsonPrimitive(instance.oAuthClientId)
            SSO_URL -> instance.ssoUrl?.let(::JsonPrimitive)
            SSO_FINISHED_URL -> instance.ssoFinishedUrl?.let(::JsonPrimitive)
            PLATFORM_NAME -> JsonPrimitive(instance.platformName)
            // The instance schema nests these under UI_COMPONENTS, as iOS does; here they are
            // top-level keys.
            in UI_COMPONENT_FLAGS -> config.getAsJsonObjectOrNull(UI_COMPONENTS)?.nonBlank(key)
                ?: config.nonBlank(key)
            UI_COMPONENTS -> config.getAsJsonObjectOrNull(UI_COMPONENTS)?.let { mergedUiComponents(it) }
            in INSTANCE_KEYS -> config.nonBlank(key)
            else -> null
        }
    }

    /** Per-field: an instance that sets one course navigation flag keeps the app's others. */
    private fun mergedUiComponents(instanceComponents: JsonObject): JsonObject {
        val merged = super.getObject(UI_COMPONENTS)
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.deepCopy()
            ?: JsonObject()
        instanceComponents.entrySet().forEach { (key, value) -> merged.add(key, value) }
        return merged
    }

    companion object {
        private val UI_COMPONENT_FLAGS = setOf(
            LOGIN_REGISTRATION_ENABLED,
            SAML_SSO_LOGIN_ENABLED,
            SAML_SSO_DEFAULT_LOGIN_BUTTON,
        )

        /** App-level keys an instance may override. */
        val INSTANCE_KEYS = setOf(
            API_HOST_URL,
            OAUTH_CLIENT_ID,
            SSO_URL,
            SSO_FINISHED_URL,
            TOKEN_TYPE,
            FEEDBACK_EMAIL_ADDRESS,
            FAQ_URL,
            PLATFORM_NAME,
            AGREEMENT_URLS,
            DISCOVERY,
            PROGRAM,
            DASHBOARD,
            UI_COMPONENTS,
            SSO_BUTTON_TITLE,
            EXPERIMENTAL_FEATURES,
        ) + UI_COMPONENT_FLAGS

        private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? =
            get(key)?.takeIf { it.isJsonObject }?.asJsonObject

        /** A set value; `null` and blank strings count as unset. */
        private fun JsonObject.nonBlank(key: String): JsonElement? {
            val value = get(key) ?: return null
            return when {
                value.isJsonNull -> null
                value.isJsonPrimitive && value.asJsonPrimitive.isString && value.asString.isBlank() -> null
                else -> value
            }
        }
    }
}
