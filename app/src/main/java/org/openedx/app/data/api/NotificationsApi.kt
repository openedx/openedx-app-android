package org.openedx.app.data.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface NotificationsApi {
    @POST("/api/mobile/v4/notifications/create-token/")
    @FormUrlEncoded
    suspend fun syncFirebaseToken(
        @Field("registration_id") token: String,
        @Field("active") active: Boolean = true
    )
}
