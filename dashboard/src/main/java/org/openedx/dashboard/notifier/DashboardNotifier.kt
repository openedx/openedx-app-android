package org.openedx.dashboard.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DashboardNotifier {

    private val channel = MutableSharedFlow<DashboardEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<DashboardEvent> = channel.asSharedFlow()

    suspend fun send(event: DashboardEvent) = channel.emit(event)
}
