package com.raccoongang.discussion.system.notifier

import com.raccoongang.discussion.domain.model.DiscussionComment


data class DiscussionCommentAdded(
    val comment: DiscussionComment
) : DiscussionEvent