package org.openedx.app.data.networking

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.app.BuildConfig
import org.openedx.core.BuildConfig.ACCESS_TOKEN_TYPE
import org.openedx.core.data.storage.CorePreferences

class HeadersInterceptor(private val context: Context, private val preferencesManager: CorePreferences) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder().apply {
                    val token = preferencesManager.accessToken

                    if (token.isNotEmpty()) {
                        addHeader("Authorization", "$ACCESS_TOKEN_TYPE $token")
                    }

                    addHeader("Accept", "application/json")
                    addHeader(
                        "User-Agent", System.getProperty("http.agent") + " " +
                                context.getString(org.openedx.core.R.string.app_name) + "/" +
                                BuildConfig.APPLICATION_ID + "/" +
                                BuildConfig.VERSION_NAME
                    )
                }.build()
        )
    }
}