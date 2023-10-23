package org.openedx.app.data.networking

import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.BuildConfig.ACCESS_TOKEN_TYPE

class HeadersInterceptor(private val preferencesManager: CorePreferences) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder().apply {
                    val token = preferencesManager.accessToken

                    if (token.isNotEmpty()) {
                        addHeader("Authorization", "$ACCESS_TOKEN_TYPE $token")
                    }

                    addHeader("Accept", "application/json")
                }.build()
        )
    }
}