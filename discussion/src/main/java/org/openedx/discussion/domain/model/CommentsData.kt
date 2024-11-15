package org.openedx.discussion.domain.model

import org.openedx.core.domain.model.Pagination

data class CommentsData(
    val results: List<DiscussionComment>,
    val pagination: Pagination
)
