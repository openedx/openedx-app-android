package org.openedx.settings.data.api

import okhttp3.ResponseBody
import org.openedx.core.ApiConstants
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SettingsApi {

    @FormUrlEncoded
    @POST(ApiConstants.URL_REVOKE_TOKEN)
    suspend fun revokeAccessToken(
        @Field("client_id") clientId: String?,
        @Field("token") token: String?,
        @Field("token_type_hint") tokenTypeHint: String?
    ): ResponseBody
}