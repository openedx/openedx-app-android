package org.openedx.discussion.data.model.response

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.Pagination
import org.openedx.core.data.model.ProfileImage
import org.openedx.core.extension.TextConverter
import org.openedx.discussion.domain.model.CommentsData
import org.openedx.discussion.domain.model.DiscussionComment

data class CommentsResponse(
    @SerializedName("results")
    val results: List<CommentResult>,
    @SerializedName("pagination")
    val pagination: Pagination
) {
    fun mapToDomain(): CommentsData {
        return CommentsData(
            results.map { it.mapToDomain() },
            pagination.mapToDomain()
        )
    }
}

data class CommentResult(
    @SerializedName("id")
    val id: String,
    @SerializedName("author")
    val author: String,
    @SerializedName("author_label")
    val authorLabel: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("raw_body")
    val rawBody: String,
    @SerializedName("rendered_body")
    val renderedBody: String,
    @SerializedName("abuse_flagged")
    val abuseFlagged: Boolean,
    @SerializedName("voted")
    val voted: Boolean,
    @SerializedName("vote_count")
    val voteCount: Int,
    @SerializedName("editable_fields")
    val editableFields: List<String>,
    @SerializedName("can_delete")
    val canDelete: Boolean,
    @SerializedName("thread_id")
    val threadId: String,
    @SerializedName("parent_id")
    val parentId: String?,
    @SerializedName("endorsed")
    val endorsed: Boolean,
    @SerializedName("endorsed_by")
    val endorsedBy: String?,
    @SerializedName("endorsed_by_label")
    val endorsedByLabel: String?,
    @SerializedName("endorsed_at")
    val endorsedAt: String?,
    @SerializedName("child_count")
    val childCount: Int,
    @SerializedName("children")
    val children: List<String>,
    @SerializedName("abuse_flagged_any_user")
    val abuseFlaggedAnyUser: String?,
    @SerializedName("profile_image")
    val profileImage: ProfileImage?,
    @SerializedName("users")
    val users: Map<String, ThreadsResponse.Thread.DiscussionProfile>?
) {
    fun mapToDomain(): DiscussionComment {
        return DiscussionComment(
            id,
            author,
            authorLabel ?: "",
            createdAt,
            updatedAt,
            rawBody,
            renderedBody,
            TextConverter.textToLinkedImageText(renderedBody),
            abuseFlagged,
            voted,
            voteCount,
            editableFields,
            canDelete,
            threadId,
            parentId ?: "",
            endorsed,
            endorsedBy ?: "",
            endorsedByLabel ?: "",
            endorsedAt ?: "",
            childCount,
            children,
            profileImage?.mapToDomain(),
            users?.entries?.associate { it.key to it.value.mapToDomain() }
        )
    }
}
