package org.openedx.discussion.system.notifier

class DiscussionThreadDataChanged(
    val thread: org.openedx.discussion.domain.model.Thread
) : DiscussionEvent
