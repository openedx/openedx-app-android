package org.openedx.core.data.api

import okhttp3.ResponseBody
import org.openedx.core.data.model.*
import retrofit2.http.*

interface CourseApi {

    @GET("/api/mobile/v3/users/{username}/course_enrollments/")
    suspend fun getEnrolledCourses(
        @Header("Cache-Control") cacheControlHeaderParam: String? = null,
        @Path("username") username: String,
        @Query("org") org: String? = null,
        @Query("page") page: Int
    ): CourseEnrollments

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

    @GET(
        "/api/mobile/{api_version}/course_info/blocks/?" +
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

    @GET("/api/course_home/v1/dates/{course_id}")
    suspend fun getCourseDates(@Path("course_id") courseId: String): CourseDates

    @POST("/api/course_experience/v1/reset_course_deadlines")
    suspend fun resetCourseDates(@Body courseBody: Map<String, String>): ResetCourseDates

    @GET("/api/course_experience/v1/course_deadlines_info/{course_id}")
    suspend fun getDatesBannerInfo(@Path("course_id") courseId: String): CourseDatesBannerInfo

    @GET("/api/mobile/v1/course_info/{course_id}/handouts")
    suspend fun getHandouts(@Path("course_id") courseId: String): HandoutsModel

    @GET("/api/mobile/v1/course_info/{course_id}/updates")
    suspend fun getAnnouncements(@Path("course_id") courseId: String): List<AnnouncementModel>
}
