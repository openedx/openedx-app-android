package com.raccoongang.discussion.system.notifier

import com.raccoongang.discussion.domain.model.DiscussionComment

class DiscussionCommentDataChanged(val discussionComment: DiscussionComment) : DiscussionEvent