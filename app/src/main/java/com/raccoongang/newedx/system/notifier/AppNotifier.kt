package com.raccoongang.newedx.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppNotifier {

    private val channel = MutableSharedFlow<AppEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<AppEvent> = channel.asSharedFlow()

    suspend fun send(event: LogoutEvent) = channel.emit(event)

}