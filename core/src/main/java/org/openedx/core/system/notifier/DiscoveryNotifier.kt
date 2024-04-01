package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DiscoveryNotifier {

    private val channel = MutableSharedFlow<DiscoveryEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<DiscoveryEvent> = channel.asSharedFlow()

    suspend fun send(event: CourseDashboardUpdate) = channel.emit(event)
}
