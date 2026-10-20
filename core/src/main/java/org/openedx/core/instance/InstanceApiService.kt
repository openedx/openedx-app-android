package org.openedx.core.instance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface InstanceApiService {
    /** The catalog's raw JSON. Throws when the request fails or no catalog URL is configured. */
    suspend fun fetchCatalog(): String
}

class InstanceCatalogNotConfiguredException : IOException("INSTANCES_CATALOG_URL is not configured")

class InstanceCatalogHttpException(val code: Int) : IOException("Catalog request failed with HTTP $code")

/**
 * Fetches the remote catalog with a plain client of its own. It is a fixed, anonymous endpoint,
 * so it deliberately bypasses the app's API client and never carries credentials.
 */
class OkHttpInstanceApiService(
    private val catalogUrl: String?,
    private val client: OkHttpClient = defaultClient(),
) : InstanceApiService {

    override suspend fun fetchCatalog(): String = withContext(Dispatchers.IO) {
        val url = catalogUrl?.takeIf { it.isNotBlank() } ?: throw InstanceCatalogNotConfiguredException()
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw InstanceCatalogHttpException(response.code)
            response.body?.string() ?: throw InstanceCatalogHttpException(response.code)
        }
    }

    companion object {
        private const val TIMEOUT_SECONDS = 10L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
