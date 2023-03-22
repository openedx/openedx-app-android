package com.raccoongang.discussion.presentation.threads

sealed class DiscussionThreadsUIState() {
    class Threads(val data: List<com.raccoongang.discussion.domain.model.Thread>) :
        DiscussionThreadsUIState()

    object Loading : DiscussionThreadsUIState()
}