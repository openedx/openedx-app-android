package com.raccoongang.discussion.presentation.topics

import com.raccoongang.discussion.domain.model.Topic


sealed class DiscussionTopicsUIState {
    data class Topics(val data: List<Topic>) : DiscussionTopicsUIState()
    object Loading : DiscussionTopicsUIState()
}