package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DownloadNotifier {

    private val channel = MutableSharedFlow<DownloadEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<DownloadEvent> = channel.asSharedFlow()

    suspend fun send(event: DownloadProgressChanged) = channel.emit(event)
    suspend fun send(event: DownloadFailed) = channel.emit(event)
}
