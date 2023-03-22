package com.raccoongang.discussion.system.notifier

import com.raccoongang.discussion.domain.model.DiscussionComment

data class DiscussionResponseAdded(
    val comment: DiscussionComment
) : DiscussionEvent
