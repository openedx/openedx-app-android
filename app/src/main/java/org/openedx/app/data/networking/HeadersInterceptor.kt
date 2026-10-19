package org.openedx.app.data.networking

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.presentation.global.AppData

class HeadersInterceptor(
    private val appData: AppData,
    private val config: Config,
    private val preferencesManager: CorePreferences,
    private val instanceProvider: InstanceProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder().apply {
                    val token = preferencesManager.accessToken

                    // The token belongs to the selected site; never send it to another host.
                    if (token.isNotEmpty() && targetsActiveSite(request().url.toString())) {
                        addHeader("Authorization", "${config.getAccessTokenType()} $token")
                    }

                    addHeader("Accept", "application/json")

                    val httpAgent = System.getProperty("http.agent") ?: ""
                    addHeader("User-Agent", "$httpAgent ${appData.appUserAgent}")
                }.build()
        )
    }

    private fun targetsActiveSite(url: String): Boolean {
        val siteUrl = instanceProvider.currentInstance?.apiHostUrl ?: config.getApiHostURL()
        val site = siteUrl.toHttpUrlOrNull()
        val request = url.toHttpUrlOrNull()
        return when {
            // An unparseable configured host can't be compared; keep the previous behavior.
            site == null -> true
            request == null -> false
            else -> request.isSameOrigin(site)
        }
    }
}
