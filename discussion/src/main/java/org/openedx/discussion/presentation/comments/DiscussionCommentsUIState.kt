package org.openedx.discussion.presentation.comments

import org.openedx.discussion.domain.model.DiscussionComment

sealed class DiscussionCommentsUIState {
    data class Success(
        val thread: org.openedx.discussion.domain.model.Thread,
        val commentsData: List<DiscussionComment>,
        val count: Int
    ) : DiscussionCommentsUIState()

    data object Loading : DiscussionCommentsUIState()
}
