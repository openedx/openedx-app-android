package org.openedx.app.data.networking

import org.openedx.core.ApiConstants
import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.app.BuildConfig
import org.openedx.core.data.storage.CorePreferences

class HeadersInterceptor(private val preferencesManager: CorePreferences) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder().apply {
                    val token = preferencesManager.accessToken

                    if (token.isNotEmpty()) {
                        addHeader("Authorization", "${ApiConstants.TOKEN_TYPE_BEARER} $token")
                    }

                    addHeader("Accept", "application/json")
                    //TODO
                    addHeader(
                     "User-Agent", System.getProperty("http.agent") + " " +
//                            context.getString(org.openedx.core.R.string.app_name) + "/" +
                                "edX" + "/" +
                                "org.edx.mobile" + "/" +
                                BuildConfig.VERSION_NAME
                    )
                }.build()
        )
    }
}