package org.openedx.core.system.notifier.calendar

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CalendarNotifier {

    private val channel = MutableSharedFlow<CalendarEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<CalendarEvent> = channel.asSharedFlow()

    suspend fun send(event: CalendarEvent) = channel.emit(event)
}
