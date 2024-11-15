package org.openedx.discussion.system.notifier

import org.openedx.discussion.domain.model.DiscussionComment

data class DiscussionCommentAdded(
    val comment: DiscussionComment
) : DiscussionEvent
