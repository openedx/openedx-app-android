package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppUpdateNotifier {

    private val channel = MutableSharedFlow<AppUpgradeEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<AppUpgradeEvent> = channel.asSharedFlow()

    suspend fun send(event: AppUpgradeEvent) = channel.emit(event)

}