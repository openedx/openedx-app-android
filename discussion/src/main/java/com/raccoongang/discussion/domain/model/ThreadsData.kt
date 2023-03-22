package com.raccoongang.discussion.domain.model

import com.raccoongang.core.domain.model.Pagination

data class ThreadsData(
    val results: List<Thread>,
    val textSearchRewrite: String,
    val pagination: Pagination
)



