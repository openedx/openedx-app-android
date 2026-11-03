package org.openedx.app.data.networking

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.RequestInstance
import org.openedx.core.presentation.global.AppData

class HeadersInterceptor(
    private val appData: AppData,
    private val config: Config,
    private val preferencesManager: CorePreferences,
    private val instanceProvider: InstanceProvider,
    private val instanceByKey: (String) -> Instance? = { null },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        val request = request()
        proceed(
            request.newBuilder().apply {
                // The token of the site the request belongs to, and only to that site's host.
                val site = request.tag(RequestInstance::class.java)
                    ?: RequestInstance(instanceProvider.currentInstance?.key)
                val token = preferencesManager.accessToken(site.instanceKey)
                if (token.isNotEmpty() && request.header(AUTHORIZATION) == null && targets(request, site)) {
                    addHeader(AUTHORIZATION, "${config.getAccessTokenType()} $token")
                }

                addHeader("Accept", "application/json")

                val httpAgent = System.getProperty("http.agent") ?: ""
                addHeader("User-Agent", "$httpAgent ${appData.appUserAgent}")
            }.build()
        )
    }

    private fun targets(request: Request, site: RequestInstance): Boolean {
        val siteUrl = when (val key = site.instanceKey) {
            null -> config.getApiHostURL()
            // A site no longer in the catalog has no known host: send its token nowhere.
            else -> instanceByKey(key)?.apiHostUrl ?: return false
        }
        val siteHost = siteUrl.toHttpUrlOrNull()
        return when {
            // An unparseable configured host can't be compared; keep the previous behavior.
            siteHost == null -> true
            else -> request.url.isSameOrigin(siteHost)
        }
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
