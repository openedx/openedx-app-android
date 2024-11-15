package org.openedx.profile.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.openedx.core.ApiConstants
import org.openedx.profile.data.model.Account
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {

    @FormUrlEncoded
    @POST(ApiConstants.URL_REVOKE_TOKEN)
    suspend fun revokeAccessToken(
        @Field("client_id") clientId: String?,
        @Field("token") token: String?,
        @Field("token_type_hint") tokenTypeHint: String?
    ): ResponseBody

    @GET("/api/user/v1/accounts/{username}")
    suspend fun getAccount(@Path("username") username: String): Account

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/user/v1/accounts/{username}")
    @JvmSuppressWildcards
    suspend fun updateAccount(
        @Path("username") username: String,
        @Body fields: Map<String, Any?>
    ): Account

    @Headers("Cache-Control: no-cache")
    @POST("/api/user/v1/accounts/{username}/image")
    suspend fun setProfileImage(
        @Path("username") username: String?,
        @Header("Content-Disposition") contentDisposition: String?,
        @Query("mobile") mobile: Boolean = true,
        @Body file: RequestBody?
    ): Response<Unit>

    @Headers("Cache-Control: no-cache")
    @DELETE("/api/user/v1/accounts/{username}/image")
    suspend fun deleteProfileImage(@Path("username") username: String?): Response<Unit>

    @FormUrlEncoded
    @POST("/api/user/v1/accounts/deactivate_logout/")
    suspend fun deactivateAccount(
        @Field("password") password: String
    ): Response<Unit>
}
