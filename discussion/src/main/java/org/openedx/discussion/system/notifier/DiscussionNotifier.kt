package org.openedx.discussion.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DiscussionNotifier {

    private val channel = MutableSharedFlow<DiscussionEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<DiscussionEvent> = channel.asSharedFlow()

    suspend fun send(event: DiscussionResponseAdded) = channel.emit(event)
    suspend fun send(event: DiscussionCommentAdded) = channel.emit(event)
    suspend fun send(event: DiscussionCommentDataChanged) = channel.emit(event)
    suspend fun send(event: DiscussionThreadDataChanged) = channel.emit(event)
    suspend fun send(event: DiscussionThreadAdded) = channel.emit(event)
}
