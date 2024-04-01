package org.openedx.discovery.data.api

import okhttp3.ResponseBody
import org.openedx.core.data.model.CourseDetails
import org.openedx.core.data.model.CourseList
import org.openedx.core.data.model.EnrollBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscoveryApi {

    @GET("/api/courses/v1/courses/")
    suspend fun getCourseList(
        @Query("search_term") searchQuery: String? = null,
        @Query("page") page: Int,
        @Query("mobile") mobile: Boolean,
        @Query("mobile_search") mobileSearch: Boolean,
        @Query("username") username: String? = null,
        @Query("org") org: String? = null,
        @Query("permissions") permission: List<String> = listOf(
            "enroll",
            "see_in_catalog",
            "see_about_page"
        )
    ): CourseList

    @GET("/api/courses/v1/courses/{course_id}")
    suspend fun getCourseDetail(
        @Path("course_id") courseId: String?,
        @Query("username") username: String? = null
    ): CourseDetails

    @POST("/api/enrollment/v1/enrollment")
    suspend fun enrollInACourse(@Body enrollBody: EnrollBody): ResponseBody
}
