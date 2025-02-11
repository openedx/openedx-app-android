package org.openedx.core.data.api

import okhttp3.RequestBody
import org.openedx.core.ApiConstants
import retrofit2.Response
import retrofit2.http.POST

interface CookiesApi {
    @POST(ApiConstants.URL_LOGIN)
    suspend fun userCookies(): Response<RequestBody>
}
