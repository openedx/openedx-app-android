package org.openedx.discussion.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.LinkedImageText
import org.openedx.discussion.R

@Parcelize
data class Thread(
    val id: String,
    val author: String,
    val authorLabel: String,
    val createdAt: String,
    val updatedAt: String,
    val rawBody: String,
    val renderedBody: String,
    val parsedRenderedBody: LinkedImageText,
    val abuseFlagged: Boolean,
    val voted: Boolean,
    val voteCount: Int,
    val editableFields: List<String>,
    val canDelete: Boolean,
    val courseId: String,
    val topicId: String,
    val groupId: String,
    val groupName: String,
    val type: DiscussionType,
    val previewBody: String,
    val abuseFlaggedCount: String,
    val title: String,
    val pinned: Boolean,
    val closed: Boolean,
    val following: Boolean,
    val commentCount: Int,
    val unreadCommentCount: Int,
    val read: Boolean,
    val hasEndorsed: Boolean,
    val users: Map<String, DiscussionProfile>?,
    val responseCount: Int,
    val anonymous: Boolean,
    val anonymousToPeers: Boolean
) : Parcelable

@Parcelize
data class DiscussionProfile(
    val image: ProfileImage?
) : Parcelable

enum class DiscussionType(
    val value: String,
    val resId: Int
) {
    QUESTION("question", R.string.discussion_question),
    DISCUSSION("discussion", R.string.discussion_discussion)
}
