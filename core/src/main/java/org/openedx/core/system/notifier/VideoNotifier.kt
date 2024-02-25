package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class VideoNotifier {

    private val channel = MutableSharedFlow<VideoEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<VideoEvent> = channel.asSharedFlow()

    suspend fun send(event: VideoQualityChanged) = channel.emit(event)
}
