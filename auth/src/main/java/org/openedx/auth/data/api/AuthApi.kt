package org.openedx.auth.data.api

import org.openedx.auth.data.model.AuthResponse
import org.openedx.auth.data.model.PasswordResetResponse
import org.openedx.auth.data.model.RegistrationFields
import org.openedx.auth.data.model.ValidationFields
import org.openedx.core.ApiConstants
import org.openedx.core.data.model.*
import retrofit2.Call
import retrofit2.http.*

interface AuthApi {

    @FormUrlEncoded
    @POST(ApiConstants.URL_EXCHANGE_TOKEN)
    suspend fun exchangeAccessToken(
        @Field("access_token") accessToken: String,
        @Field("client_id") clientId: String,
        @Field("token_type") tokenType: String,
        @Field("asymmetric_jwt") isAsymmetricJwt: Boolean = true,
        @Path("auth_type") authType: String,
    ): AuthResponse

    @FormUrlEncoded
    @POST(ApiConstants.URL_ACCESS_TOKEN)
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("token_type") tokenType: String,
        @Field("asymmetric_jwt") isAsymmetricJwt: Boolean = true,
    ): AuthResponse

    @FormUrlEncoded
    @POST(ApiConstants.URL_ACCESS_TOKEN)
    fun refreshAccessToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("token_type") tokenType: String,
        @Field("asymmetric_jwt") isAsymmetricJwt: Boolean = true,
    ): Call<AuthResponse>

    @GET(ApiConstants.URL_REGISTRATION_FIELDS)
    suspend fun getRegistrationFields(): RegistrationFields

    @FormUrlEncoded
    @POST(ApiConstants.URL_REGISTER)
    suspend fun registerUser(@FieldMap fields: Map<String, String>)

    @FormUrlEncoded
    @POST(ApiConstants.URL_VALIDATE_REGISTRATION_FIELDS)
    suspend fun validateRegistrationFields(@FieldMap fields: Map<String, String>): ValidationFields

    @GET(ApiConstants.GET_USER_PROFILE)
    suspend fun getProfile(): User

    @FormUrlEncoded
    @POST(ApiConstants.URL_PASSWORD_RESET)
    suspend fun passwordReset(@Field("email") email: String): PasswordResetResponse
}