package org.openedx.discussion.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.LinkedImageText

@Parcelize
data class DiscussionComment(
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
    val threadId: String,
    val parentId: String,
    val endorsed: Boolean,
    val endorsedBy: String,
    val endorsedByLabel: String,
    val endorsedAt: String,
    val childCount: Int,
    val children: List<String>,
    val profileImage: ProfileImage?,
    val users: Map<String, DiscussionProfile>?
) : Parcelable
