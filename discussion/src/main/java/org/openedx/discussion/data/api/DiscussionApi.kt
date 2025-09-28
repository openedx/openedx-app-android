package org.openedx.discussion.data.api

import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.discussion.data.model.request.CommentBody
import org.openedx.discussion.data.model.request.FollowBody
import org.openedx.discussion.data.model.request.ReadBody
import org.openedx.discussion.data.model.request.ReportBody
import org.openedx.discussion.data.model.request.ThreadBody
import org.openedx.discussion.data.model.request.VoteBody
import org.openedx.discussion.data.model.response.CommentResult
import org.openedx.discussion.data.model.response.CommentsResponse
import org.openedx.discussion.data.model.response.ThreadsResponse
import org.openedx.discussion.data.model.response.ThreadsResponse.Thread
import org.openedx.discussion.data.model.response.TopicsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscussionApi {

    @GET("/api/discussion/v1/course_topics/{course_id}")
    suspend fun getCourseTopics(
        @Path("course_id") courseId: String,
    ): TopicsResponse

    @GET("/api/discussion/v1/threads/")
    suspend fun getCourseThreads(
        @Query("course_id") courseId: String,
        @Query("following") following: Boolean?,
        @Query("topic_id") topicId: String?,
        @Query("order_by") orderBy: String,
        @Query("view") view: String?,
        @Query("page") page: Int = 1,
        @Query("requested_fields") requestedFields: List<String> = listOf("profile_image")
    ): ThreadsResponse

    @GET("/api/discussion/v1/threads/{thread_id}")
    suspend fun getCourseThread(
        @Path("thread_id") threadId: String,
        @Query("course_id") courseId: String,
        @Query("topic_id") topicId: String,
        @Query("requested_fields") requestedFields: List<String> = listOf("profile_image")
    ): Thread

    @GET("/api/discussion/v1/threads/")
    suspend fun searchThreads(
        @Query("course_id") courseId: String,
        @Query("text_search") query: String,
        @Query("page") page: Int = 1,
        @Query("requested_fields") requestedFields: List<String> = listOf("profile_image")
    ): ThreadsResponse

    @GET("/api/discussion/v1/comments/")
    suspend fun getThreadComments(
        @Query("thread_id") threadId: String,
        @Query("page") page: Int,
        @Query("requested_fields") requestedFields: List<String> = listOf("profile_image")
    ): CommentsResponse

    @Headers("Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/comments/{response_id}/")
    suspend fun getResponse(
        @Path("response_id") responseId: String
    ): CommentResult

    @GET("/api/discussion/v1/comments/")
    suspend fun getThreadQuestionComments(
        @Query("thread_id") threadId: String,
        @Query("page") page: Int,
        @Query("endorsed") endorsed: Boolean,
        @Query("requested_fields") requestedFields: List<String> = listOf("profile_image")
    ): CommentsResponse

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/threads/{thread_id}/")
    suspend fun setThreadRead(
        @Path("thread_id") threadId: String,
        @Body body: ReadBody
    ): Thread

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/threads/{thread_id}/")
    suspend fun setThreadVoted(
        @Path("thread_id") threadId: String,
        @Body body: VoteBody
    ): Thread

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/threads/{thread_id}/")
    suspend fun setThreadFlagged(
        @Path("thread_id") threadId: String,
        @Body reportBody: ReportBody
    ): Thread

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/threads/{thread_id}/")
    suspend fun setThreadFollowed(
        @Path("thread_id") threadId: String,
        @Body followBody: FollowBody
    ): Thread

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/comments/{comment_id}/")
    suspend fun setCommentVoted(
        @Path("comment_id") commentId: String,
        @Body voteBody: VoteBody
    ): CommentResult

    @Headers("Cache-Control: no-cache", "Content-type: application/merge-patch+json")
    @PATCH("/api/discussion/v1/comments/{comment_id}/")
    suspend fun setCommentFlagged(
        @Path("comment_id") commentId: String,
        @Body reportBody: ReportBody
    ): CommentResult

    @GET("/api/discussion/v1/comments/{comment_id}/")
    suspend fun getCommentsResponses(
        @Path("comment_id") commentId: String,
        @Query("page") page: Int,
        @Query("requested_fields") requestedFields: List<String> = listOf("profile_image")
    ): CommentsResponse

    @POST("/api/discussion/v1/comments/")
    suspend fun createComment(
        @Body commentBody: CommentBody
    ): CommentResult

    @POST("/api/discussion/v1/threads/")
    suspend fun createThread(@Body threadBody: ThreadBody): Thread

    @POST("/api/completion/v1/completion-batch")
    suspend fun markBlocksCompletion(
        @Body
        blocksCompletionBody: BlocksCompletionBody
    )
}
