package org.openedx.app.data.networking

import android.util.Log
import com.google.gson.Gson
import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.data.model.AuthResponse
import org.openedx.core.ApiConstants
import org.openedx.app.system.notifier.AppNotifier
import org.openedx.app.system.notifier.LogoutEvent
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import org.openedx.core.BuildConfig
import org.openedx.core.data.storage.CorePreferences
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class OauthRefreshTokenAuthenticator(
    private val preferencesManager: CorePreferences,
    private val appNotifier: AppNotifier,
) : Authenticator {

    private val authApi: AuthApi

    init {
        val okHttpClient = OkHttpClient.Builder().apply {
            writeTimeout(60, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
        }.build()
        authApi = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(AuthApi::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val accessToken = preferencesManager.accessToken
        val refreshToken = preferencesManager.refreshToken

        if (refreshToken.isEmpty()) {
            return null
        }

        val errorCode = getErrorCode(response.peekBody(200).string())
        if (errorCode != null) {
            when (errorCode) {
                TOKEN_EXPIRED_ERROR_MESSAGE -> {
                    try {
                        val newAuth = refreshAccessToken(refreshToken)
                        if (newAuth != null) {
                            return response.request.newBuilder()
                                .header(
                                    "Authorization",
                                    ApiConstants.TOKEN_TYPE_BEARER + " " + newAuth.accessToken
                                )
                                .build()
                        } else {
                            val actualToken = preferencesManager.accessToken
                            if (actualToken != accessToken) {
                                return response.request.newBuilder()
                                    .header(
                                        "Authorization",
                                        ApiConstants.TOKEN_TYPE_BEARER + " " + actualToken
                                    )
                                    .build()
                            }
                            return null
                        }

                    } catch (e: Exception) {
                        return null
                    }
                }
                TOKEN_NONEXISTENT_ERROR_MESSAGE, TOKEN_INVALID_GRANT_ERROR_MESSAGE -> {
                    // Retry request with the current access_token if the original access_token used in
                    // request does not match the current access_token. This case can occur when
                    // asynchronous calls are made and are attempting to refresh the access_token where
                    // one call succeeds but the other fails. https://github.com/edx/edx-app-android/pull/834
                    val authHeaders =
                        response.request.headers["Authorization"]?.split(" ".toRegex())
                    if (authHeaders?.toTypedArray()?.getOrNull(1) != accessToken) {
                        return response.request.newBuilder()
                            .header(
                                "Authorization",
                                ApiConstants.TOKEN_TYPE_BEARER + " " + accessToken
                            ).build()
                    }

                    runBlocking {
                        appNotifier.send(LogoutEvent())
                    }
                }
                DISABLED_USER_ERROR_MESSAGE -> {
                    runBlocking {
                        appNotifier.send(LogoutEvent())
                    }
                }
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun refreshAccessToken(refreshToken: String): AuthResponse? {
        val response = authApi.refreshAccessToken(
            ApiConstants.TOKEN_TYPE_REFRESH,
            BuildConfig.CLIENT_ID,
            refreshToken
        ).execute()
        val authResponse = response.body()
        if (response.isSuccessful && authResponse != null) {
            val newAccessToken = authResponse.accessToken ?: ""
            val newRefreshToken = authResponse.refreshToken ?: ""

            if (newAccessToken.isNotEmpty() && newRefreshToken.isNotEmpty()) {
                preferencesManager.accessToken = newAccessToken
                preferencesManager.refreshToken = newRefreshToken
            }
        } else if (response.code() == 400) {
            //another refresh already in progress
            Thread.sleep(1500)
        }

        return authResponse
    }

    private fun getErrorCode(responseBody: String): String? {
        try {
            val jsonObj = JSONObject(responseBody)
            var errorCode = jsonObj.optString("error_code", "")
            return if (errorCode != "") {
                errorCode
            } else {
                errorCode = jsonObj
                    .optJSONObject("developer_message")
                    ?.optString("error_code", "") ?: ""
                if (errorCode != "") {
                    errorCode
                } else {
                    null
                }
            }
        } catch (ex: JSONException) {
            Log.d("OauthRefreshTokenAuthenticator", "Unable to get error_code from 401 response")
            return null
        }
    }

    companion object {
        private const val TOKEN_EXPIRED_ERROR_MESSAGE = "token_expired"
        private const val TOKEN_NONEXISTENT_ERROR_MESSAGE = "token_nonexistent"
        private const val TOKEN_INVALID_GRANT_ERROR_MESSAGE = "invalid_grant"
        private const val DISABLED_USER_ERROR_MESSAGE = "user_is_disabled"
    }
}