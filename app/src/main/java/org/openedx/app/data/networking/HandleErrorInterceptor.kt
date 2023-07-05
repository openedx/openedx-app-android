package org.openedx.app.data.networking

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.openedx.core.data.model.ErrorResponse
import org.openedx.core.system.EdxError
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException

class HandleErrorInterceptor(
    private val gson: Gson
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        val responseCode = response.code
        if (responseCode in 400..500 && response.body != null) {
            val jsonStr = response.body!!.string()

            try {
                val errorResponse = gson.fromJson(jsonStr, ErrorResponse::class.java)
                if (errorResponse?.error != null) {
                    when (errorResponse.error) {
                        ERROR_INVALID_GRANT -> {
                            throw EdxError.InvalidGrantException()
                        }
                        ERROR_USER_NOT_ACTIVE -> {
                            throw EdxError.UserNotActiveException()
                        }
                        else -> {
                            return response
                        }
                    }
                } else if (errorResponse.errorDescription != null) {
                    throw EdxError.ValidationException(errorResponse.errorDescription ?: "")
                }
            } catch (e: JsonSyntaxException) {
                throw IOException("JsonSyntaxException $jsonStr", e)
            }
        }

        return response
    }

    companion object {
        const val ERROR_INVALID_GRANT = "invalid_grant"
        const val ERROR_USER_NOT_ACTIVE = "user_not_active"
    }
}