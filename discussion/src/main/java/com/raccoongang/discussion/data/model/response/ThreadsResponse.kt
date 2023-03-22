package com.raccoongang.discussion.data.model.response

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.Pagination
import com.raccoongang.core.data.model.ProfileImage
import com.raccoongang.core.extension.TextConverter
import com.raccoongang.discussion.domain.model.DiscussionType
import com.raccoongang.discussion.domain.model.ThreadsData

data class ThreadsResponse(
    @SerializedName("results")
    val results: List<Thread>,
    @SerializedName("text_search_rewrite")
    val textSearchRewrite: String?,
    @SerializedName("pagination")
    val pagination: Pagination
) {
    data class Thread(
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
        @SerializedName("course_id")
        val courseId: String,
        @SerializedName("topic_id")
        val topicId: String,
        @SerializedName("group_id")
        val groupId: String?,
        @SerializedName("group_name")
        val groupName: String?,
        @SerializedName("type")
        val type: String,
        @SerializedName("preview_body")
        val previewBody: String,
        @SerializedName("abuse_flagged_count")
        val abuseFlaggedCount: Any?,
        @SerializedName("title")
        val title: String,
        @SerializedName("pinned")
        val pinned: Boolean,
        @SerializedName("closed")
        val closed: Boolean,
        @SerializedName("following")
        val following: Boolean,
        @SerializedName("comment_count")
        val commentCount: Int,
        @SerializedName("unread_comment_count")
        val unreadCommentCount: Int,
        @SerializedName("read")
        val read: Boolean,
        @SerializedName("has_endorsed")
        val hasEndorsed: Boolean,
        @SerializedName("users")
        val users: Map<String, DiscussionProfile>?
    ) {
        data class DiscussionProfile(
            @SerializedName("profile")
            val profile: ProfileResponse
        ) {
            fun mapToDomain(): com.raccoongang.discussion.domain.model.DiscussionProfile {
                return com.raccoongang.discussion.domain.model.DiscussionProfile(
                    image = profile.image.mapToDomain()
                )
            }
        }

        data class ProfileResponse(
            @SerializedName("image")
            val image: ProfileImage
        )

        fun mapToDomain(): com.raccoongang.discussion.domain.model.Thread {
            return com.raccoongang.discussion.domain.model.Thread(
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
                courseId,
                topicId,
                groupId ?: "",
                groupName ?: "",
                serverTypeToLocalType(),
                previewBody,
                "",
                title,
                pinned,
                closed,
                following,
                commentCount,
                unreadCommentCount,
                read,
                hasEndorsed,
                users?.entries?.associate { it.key to it.value.mapToDomain() }
            )
        }

        fun serverTypeToLocalType(): DiscussionType {
            val actualType = if (type.contains("-")) {
                type.replace("-", "_")
            } else type
            return try {
                DiscussionType.valueOf(actualType.uppercase())
            } catch (e: Exception) {
                throw IllegalStateException("Unknown thread type")
            }
        }

    }

    fun mapToDomain(): ThreadsData {
        return ThreadsData(
            results.map { it.mapToDomain() },
            textSearchRewrite ?: "",
            pagination.mapToDomain()
        )
    }

}

