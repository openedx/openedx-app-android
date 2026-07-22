package org.openedx.app.data.networking

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.core.data.storage.CorePreferences

/**
 * LMS Directory: routes every API request to the platform the learner selected.
 *
 * The Retrofit client is built once with the config host, but the selected LMS can
 * differ (and is chosen after the client exists). This rewrites each request's
 * scheme/host/port to [CorePreferences.selectedBaseUrl] on the fly. No selection
 * (feature off, or a stock build) → requests pass through untouched.
 */
class BaseUrlOverrideInterceptor(
    private val corePreferences: CorePreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val override = corePreferences.selectedBaseUrl
        val original = chain.request()

        if (override.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val baseUrl = override.toHttpUrlOrNull()
        val originalUrl = original.url

        val needsUpdate = baseUrl != null && (
            originalUrl.host != baseUrl.host ||
                originalUrl.port != baseUrl.port ||
                originalUrl.scheme != baseUrl.scheme
            )

        val requestToProcess = if (needsUpdate && baseUrl != null) {
            val updatedUrl = originalUrl.newBuilder()
                .scheme(baseUrl.scheme)
                .host(baseUrl.host)
                .port(baseUrl.port)
                .build()
            original.newBuilder().url(updatedUrl).build()
        } else {
            original
        }

        return chain.proceed(requestToProcess)
    }
}
