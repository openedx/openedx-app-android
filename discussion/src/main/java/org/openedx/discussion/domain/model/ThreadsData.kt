package org.openedx.discussion.domain.model

import org.openedx.core.domain.model.Pagination

data class ThreadsData(
    val results: List<Thread>,
    val textSearchRewrite: String,
    val pagination: Pagination
)
