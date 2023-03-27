package com.raccoongang.core.data.api

import com.raccoongang.core.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.*

interface CourseApi {

    @GET("/api/mobile/v1/users/{username}/course_enrollments")
    suspend fun getEnrolledCourses(
        @Header("Cache-Control") cacheControlHeaderParam: String? = null,
        @Path("username") username: String,
        @Query("org") org: String? = null,
    ): List<EnrolledCourse>

    @GET("/api/courses/v1/courses/")
    suspend fun getCourseList(
        @Query("search_term") searchQuery: String? = null,
        @Query("page") page: Int,
        @Query("mobile") mobile: Boolean,
        @Query("username") username: String? = null,
        @Query("org") org: String? = null,
        @Query("permissions") permission: List<String> = listOf("enroll", "see_in_catalog", "see_about_page")
    ): CourseList

    @GET("/mobile_api_extensions/v1/courses/{course_id}")
    suspend fun getCourseDetail(
        @Path("course_id") courseId: String?,
        @Query("username") username: String? = null,
        @Query("is_enrolled") isEnrolled: Boolean = true,
    ): CourseDetails

    @GET(
        "/api/courses/{api_version}/blocks/?" +
                "depth=all&" +
                "requested_fields=contains_gated_content,show_gated_sections,special_exam_info,graded,format,student_view_multi_device,due,completion&" +
                "student_view_data=video,discussion&" +
                "block_counts=video&" +
                "nav_depth=3"
    )
    suspend fun getCourseStructure(
        @Header("Cache-Control") cacheControlHeaderParam: String,
        @Path("api_version") blocksApiVersion: String,
        @Query("username") username: String?,
        @Query("course_id") courseId: String,
    ): CourseStructureModel

    @POST("/api/enrollment/v1/enrollment")
    suspend fun enrollInACourse(@Body enrollBody: EnrollBody): ResponseBody

    @GET("/api/mobile/v1/users/{username}/course_status_info/{course_id}")
    suspend fun getCourseStatus(
        @Path("username") username: String,
        @Path("course_id") courseId: String,
    ): CourseComponentStatus

    @POST("/api/completion/v1/completion-batch")
    suspend fun markBlocksCompletion(
        @Body
        blocksCompletionBody: BlocksCompletionBody
    )

    @GET
    suspend fun getHandouts(@Url url: String): HandoutsModel

    @GET
    suspend fun getAnnouncements(@Url url: String): List<AnnouncementModel>
}
