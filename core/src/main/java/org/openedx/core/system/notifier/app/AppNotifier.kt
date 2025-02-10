package org.openedx.core.system.notifier.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppNotifier {

    private val channel = MutableSharedFlow<AppEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<AppEvent> = channel.asSharedFlow()

    suspend fun send(event: SignInEvent) = channel.emit(event)

    suspend fun send(event: LogoutEvent) = channel.emit(event)

    suspend fun send(event: AppUpgradeEvent) = channel.emit(event)
}
