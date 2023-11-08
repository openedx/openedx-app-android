package org.openedx.core.data.api

import okhttp3.ResponseBody
import org.openedx.core.data.model.*
import retrofit2.http.*

interface CourseApi {

    @GET("/mobile_api_extensions/v1/users/{username}/course_enrollments")
    suspend fun getEnrolledCourses(
        @Header("Cache-Control") cacheControlHeaderParam: String? = null,
        @Path("username") username: String,
        @Query("org") org: String? = null,
        @Query("page") page: Int
    ): DashboardCourseList

    @GET("/mobile_api_extensions/courses/v1/courses/")
    suspend fun getCourseList(
        @Query("search_term") searchQuery: String? = null,
        @Query("page") page: Int,
        @Query("mobile") mobile: Boolean,
        @Query("username") username: String? = null,
        @Query("org") org: String? = null,
        @Query("permissions") permission: List<String> = listOf(
            "enroll",
            "see_in_catalog",
            "see_about_page"
        )
    ): CourseList

    @GET("/mobile_api_extensions/v1/courses/{course_id}")
    suspend fun getCourseDetail(
        @Path("course_id") courseId: String?,
        @Query("username") username: String? = null,
        @Query("is_enrolled") isEnrolled: Boolean = true,
    ): CourseDetails

    @GET(
        "/mobile_api_extensions/{api_version}/blocks/?" +
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

    @GET("/api/mobile/v1/course_info/{course_id}/handouts")
    suspend fun getHandouts(@Path("course_id") courseId: String): HandoutsModel

    @GET("/api/mobile/v1/course_info/{course_id}/updates")
    suspend fun getAnnouncements(@Path("course_id") courseId: String): List<AnnouncementModel>
}
