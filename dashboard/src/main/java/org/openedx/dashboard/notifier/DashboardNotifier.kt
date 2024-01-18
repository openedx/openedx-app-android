package org.openedx.dashboard.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardNotifier {

    private val channel = MutableStateFlow<DashboardEvent>(DashboardEvent.Empty)

    val notifier: Flow<DashboardEvent> = channel.asStateFlow()

    suspend fun send(event: DashboardEvent) = channel.emit(event)
}
