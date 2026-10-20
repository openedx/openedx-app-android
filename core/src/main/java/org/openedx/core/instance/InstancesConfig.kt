package org.openedx.core.instance

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.Locale

/**
 * The parsed instance catalog: every platform the app can sign in to, in catalog order.
 */
data class InstancesConfig(
    val instances: List<Instance> = emptyList(),
) {

    fun instance(withKey: String): Instance? = instances.firstOrNull { it.key == withKey }

    companion object {
        const val INSTANCES = "INSTANCES"

        /**
         * Builds the catalog from an `INSTANCES` array. Entries that aren't objects or fail
         * [Instance.fromJson] are dropped.
         *
         * @param fallback values used for the fields an entry omits; the entry always wins.
         */
        fun fromJson(
            array: JsonArray,
            fallback: JsonObject = JsonObject(),
            languageCode: String = Locale.getDefault().language,
        ): InstancesConfig {
            val instances = array.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val merged = fallback.deepCopy()
                element.asJsonObject.entrySet().forEach { (key, value) -> merged.add(key, value) }
                Instance.fromJson(merged, languageCode)
            }
            return InstancesConfig(instances)
        }

        /** The `INSTANCES` array of the bundled `config.json`, or an empty catalog. */
        fun fromAppConfig(
            appConfig: JsonObject,
            languageCode: String = Locale.getDefault().language,
        ): InstancesConfig {
            val array = appConfig.get(INSTANCES)
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
                ?: return InstancesConfig()
            return fromJson(array, languageCode = languageCode)
        }
    }
}
