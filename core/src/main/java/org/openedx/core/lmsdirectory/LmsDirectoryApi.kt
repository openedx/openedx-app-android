package org.openedx.core.lmsdirectory

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for the LMS registry (same backend as the iOS app). */
interface LmsDirectoryApi {

    @GET("api/v1/config")
    suspend fun getConfig(): DirectoryConfigDto

    @GET("api/v1/directory")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("featured") featured: Boolean? = null,
    ): DirectoryListResponse

    @GET("api/v1/directory/{id}")
    suspend fun detail(@Path("id") id: String): LmsDetailDto

    @POST("api/v1/reports")
    suspend fun submitReport(@Body body: ReportRequestBody): ReportResponse
}
