package com.raccoongang.discussion.presentation.search

sealed class DiscussionSearchThreadUIState {
    class Threads(val data: List<com.raccoongang.discussion.domain.model.Thread>, val count: Int) :
        DiscussionSearchThreadUIState()

    object Loading : DiscussionSearchThreadUIState()
}