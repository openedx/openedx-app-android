package org.openedx.core.instance

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException

/** Keeps the last remote catalog that replaced the baseline. */
interface InstanceCatalogCache {
    var cachedCatalogJson: String?
}

class SharedPrefsInstanceCatalogCache(context: Context) : InstanceCatalogCache {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var cachedCatalogJson: String?
        get() = prefs.getString(KEY_CATALOG_JSON, null)
        set(value) = prefs.edit { putString(KEY_CATALOG_JSON, value) }

    private companion object {
        const val PREFS_NAME = "openedx_instance_catalog"
        const val KEY_CATALOG_JSON = "cached_catalog_json"
    }
}

/**
 * Decides which catalog the app runs with.
 *
 * The baseline is the last catalog fetched successfully, else the bundled one, else empty.
 * Every launch tries a live fetch: a response with at least one instance replaces the baseline
 * and becomes the new cache. A failed fetch, or one that returns no instances, leaves the
 * baseline untouched, so a reachable but empty endpoint can never wipe a good cache, and an
 * offline launch shows the catalog the learner saw last time.
 */
class InstanceConfigLoader(
    private val apiService: InstanceApiService,
    private val cache: InstanceCatalogCache,
    private val bundledConfig: () -> InstancesConfig,
    private val logger: (String) -> Unit = { Log.w(TAG, it) },
) {

    suspend fun load(): InstancesConfig {
        val baseline = loadCached() ?: bundledConfig().takeIf { it.instances.isNotEmpty() } ?: InstancesConfig()
        return try {
            val json = apiService.fetchCatalog()
            val fetched = InstancesConfig.fromRemoteJson(json)
            if (fetched.instances.isEmpty()) {
                baseline
            } else {
                cache.cachedCatalogJson = json
                fetched
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // No configured URL is the normal single-catalog setup, not a failure worth logging.
            if (e !is InstanceCatalogNotConfiguredException) {
                logger("Live catalog fetch failed (${e.message}), using the baseline")
            }
            baseline
        }
    }

    /** `null` if nothing was cached, or if the cached payload no longer parses. */
    private fun loadCached(): InstancesConfig? {
        val json = cache.cachedCatalogJson ?: return null
        return runCatching { InstancesConfig.fromRemoteJson(json) }
            .getOrNull()
            ?.takeIf { it.instances.isNotEmpty() }
    }

    private companion object {
        const val TAG = "InstanceConfigLoader"
    }
}
