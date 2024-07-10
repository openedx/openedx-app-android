package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class IAPNotifier {
    private val channel = MutableSharedFlow<IAPEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<IAPEvent> = channel.asSharedFlow()
    suspend fun send(event: UpdateCourseData) = channel.emit(event)

    suspend fun send(event: CourseDataUpdated) = channel.emit(event)
}
