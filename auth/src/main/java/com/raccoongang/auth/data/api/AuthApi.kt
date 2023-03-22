package com.raccoongang.auth.data.api

import com.raccoongang.auth.data.model.AuthResponse
import com.raccoongang.auth.data.model.PasswordResetResponse
import com.raccoongang.auth.data.model.RegistrationFields
import com.raccoongang.auth.data.model.ValidationFields
import com.raccoongang.core.ApiConstants
import com.raccoongang.core.data.model.*
import retrofit2.Call
import retrofit2.http.*

interface AuthApi {

    @FormUrlEncoded
    @POST(ApiConstants.URL_ACCESS_TOKEN)
    suspend fun getAccessToken(
        @Field("grant_type")
        grantType: String,
        @Field("client_id")
        clientId: String,
        @Field("username")
        username: String,
        @Field("password")
        password: String,
    ): AuthResponse

    @FormUrlEncoded
    @POST(ApiConstants.URL_ACCESS_TOKEN)
    fun refreshAccessToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
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