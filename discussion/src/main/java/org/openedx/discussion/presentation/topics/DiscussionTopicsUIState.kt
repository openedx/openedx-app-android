package org.openedx.discussion.presentation.topics

import org.openedx.discussion.domain.model.Topic

sealed class DiscussionTopicsUIState {
    data class Topics(val data: List<Topic>) : DiscussionTopicsUIState()
    data object Loading : DiscussionTopicsUIState()
    data object Error : DiscussionTopicsUIState()
}
