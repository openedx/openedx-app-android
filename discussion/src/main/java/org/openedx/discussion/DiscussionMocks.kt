package org.openedx.discussion

import org.openedx.core.domain.model.ProfileImage
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionProfile
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.domain.model.Topic

object DiscussionMocks {
    val topic = Topic(
        id = "topic-id",
        name = "Mock Topic",
        threadListUrl = "",
        children = emptyList()
    )

    val thread = Thread(
        id = "thread-id",
        author = "Preview Author",
        authorLabel = "staff",
        createdAt = "2024-01-01",
        updatedAt = "2024-01-02",
        rawBody = "Preview thread body",
        renderedBody = "Preview thread body",
        abuseFlagged = false,
        voted = false,
        voteCount = 0,
        editableFields = emptyList(),
        canDelete = false,
        courseId = "course-id",
        topicId = "topic-id",
        groupId = "0",
        groupName = "",
        type = DiscussionType.DISCUSSION,
        previewBody = "Preview thread body",
        abuseFlaggedCount = "0",
        title = "Preview Thread Title",
        pinned = false,
        closed = false,
        following = false,
        commentCount = 3,
        unreadCommentCount = 2,
        read = false,
        hasEndorsed = false,
        users = null,
        responseCount = 2,
        anonymous = false,
        anonymousToPeers = false
    )

    val comment = DiscussionComment(
        id = "comment-id",
        author = "Preview Commenter",
        authorLabel = "staff",
        createdAt = "2024-01-01",
        updatedAt = "2024-01-02",
        rawBody = "Preview comment",
        renderedBody = "Preview comment",
        abuseFlagged = false,
        voted = false,
        voteCount = 0,
        editableFields = emptyList(),
        canDelete = false,
        threadId = "thread-id",
        parentId = "",
        endorsed = false,
        endorsedBy = "",
        endorsedByLabel = "",
        endorsedAt = "",
        childCount = 0,
        children = emptyList(),
        profileImage = ProfileImage("", "", "", "", false),
        users = mapOf("Preview Commenter" to DiscussionProfile(ProfileImage("", "", "", "", false)))
    )
}
