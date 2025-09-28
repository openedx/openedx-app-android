package org.openedx.core.data.api

import okhttp3.MultipartBody
import org.openedx.core.data.model.AnnouncementModel
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.model.CourseComponentStatus
import org.openedx.core.data.model.CourseDates
import org.openedx.core.data.model.CourseDatesBannerInfo
import org.openedx.core.data.model.CourseEnrollmentDetails
import org.openedx.core.data.model.CourseEnrollments
import org.openedx.core.data.model.CourseProgressResponse
import org.openedx.core.data.model.CourseStructureModel
import org.openedx.core.data.model.DownloadCoursePreview
import org.openedx.core.data.model.EnrollmentStatus
import org.openedx.core.data.model.HandoutsModel
import org.openedx.core.data.model.ResetCourseDates
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface CourseApi {

    @GET("/api/mobile/v3/users/{username}/course_enrollments/")
    suspend fun getEnrolledCourses(
        @Header("Cache-Control") cacheControlHeaderParam: String? = null,
        @Path("username") username: String,
        @Query("org") org: String? = null,
        @Query("page") page: Int
    ): CourseEnrollments

    @GET(
        "/api/mobile/{api_version}/course_info/blocks/?" +
                "depth=all&" +
                "requested_fields=contains_gated_content,show_gated_sections,special_exam_info,graded,format," +
                "student_view_multi_device,due,completion&" +
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
    suspend fun getCourseDates(
        @Path("course_id") courseId: String,
        @Query("allow_not_started_courses") allowNotStartedCourses: Boolean = true
    ): CourseDates

    @POST("/api/course_experience/v1/reset_course_deadlines")
    suspend fun resetCourseDates(@Body courseBody: Map<String, String>): ResetCourseDates

    @GET("/api/course_experience/v1/course_deadlines_info/{course_id}")
    suspend fun getDatesBannerInfo(@Path("course_id") courseId: String): CourseDatesBannerInfo

    @GET("/api/mobile/v1/course_info/{course_id}/handouts")
    suspend fun getHandouts(@Path("course_id") courseId: String): HandoutsModel

    @GET("/api/mobile/v1/course_info/{course_id}/updates")
    suspend fun getAnnouncements(@Path("course_id") courseId: String): List<AnnouncementModel>

    @GET("/api/mobile/v4/users/{username}/course_enrollments/")
    suspend fun getUserCourses(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("status") status: String? = null,
        @Query("requested_fields") fields: List<String> = emptyList()
    ): CourseEnrollments

    @Multipart
    @POST("/courses/{course_id}/xblock/{block_id}/handler/xmodule_handler/problem_check")
    suspend fun submitOfflineXBlockProgress(
        @Path("course_id") courseId: String,
        @Path("block_id") blockId: String,
        @Part progress: List<MultipartBody.Part>
    )

    @GET("/api/mobile/v1/users/{username}/enrollments_status/")
    suspend fun getEnrollmentsStatus(
        @Path("username") username: String
    ): List<EnrollmentStatus>

    @GET("/api/mobile/v1/course_info/{course_id}/enrollment_details")
    suspend fun getEnrollmentDetails(
        @Path("course_id") courseId: String,
    ): CourseEnrollmentDetails

    @GET("/api/mobile/v1/download_courses/{username}")
    suspend fun getDownloadCoursesPreview(
        @Path("username") username: String
    ): List<DownloadCoursePreview>

    @GET("/api/course_home/progress/{course_id}")
    suspend fun getCourseProgress(
        @Path("course_id") courseId: String,
    ): CourseProgressResponse
}
