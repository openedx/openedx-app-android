package com.raccoongang.discussion.presentation.responses

import com.raccoongang.discussion.domain.model.DiscussionComment

sealed class DiscussionResponsesUIState {
    data class Success(
        val mainComment: DiscussionComment,
        val childComments: List<DiscussionComment>
    ) : DiscussionResponsesUIState()

    object Loading : DiscussionResponsesUIState()
}