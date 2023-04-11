package com.raccoongang.discussion.presentation.comments

import com.raccoongang.discussion.domain.model.DiscussionComment


sealed class DiscussionCommentsUIState {
    data class Success(
        val thread: com.raccoongang.discussion.domain.model.Thread,
        val commentsData: List<DiscussionComment>,
        val count: Int
    ) : DiscussionCommentsUIState()

    object Loading : DiscussionCommentsUIState()
}