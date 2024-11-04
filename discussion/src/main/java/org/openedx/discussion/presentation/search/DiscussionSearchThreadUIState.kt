package org.openedx.discussion.presentation.search

sealed class DiscussionSearchThreadUIState {
    class Threads(val data: List<org.openedx.discussion.domain.model.Thread>, val count: Int) :
        DiscussionSearchThreadUIState()

    data object Loading : DiscussionSearchThreadUIState()
}
