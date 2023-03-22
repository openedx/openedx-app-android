package com.raccoongang.discussion.domain.model

import com.raccoongang.core.domain.model.Pagination

data class CommentsData(
    val results: List<DiscussionComment>,
    val pagination: Pagination
)