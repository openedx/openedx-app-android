package org.openedx.discussion.presentation.responses

import org.openedx.discussion.domain.model.DiscussionComment

sealed class DiscussionResponsesUIState {
    data class Success(
        val mainComment: DiscussionComment,
        val childComments: List<DiscussionComment>
    ) : DiscussionResponsesUIState()

    object Loading : DiscussionResponsesUIState()
}
