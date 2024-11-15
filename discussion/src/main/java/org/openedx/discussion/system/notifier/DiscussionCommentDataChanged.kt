package org.openedx.discussion.system.notifier

import org.openedx.discussion.domain.model.DiscussionComment

class DiscussionCommentDataChanged(val discussionComment: DiscussionComment) : DiscussionEvent
