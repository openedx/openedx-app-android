package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CourseNotifier {

    private val channel = MutableSharedFlow<CourseEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<CourseEvent> = channel.asSharedFlow()

    suspend fun send(event: CourseVideoPositionChanged) = channel.emit(event)
    suspend fun send(event: CourseStructureUpdated) = channel.emit(event)
    suspend fun send(event: CourseStructureGot) = channel.emit(event)
    suspend fun send(event: CourseSubtitleLanguageChanged) = channel.emit(event)
    suspend fun send(event: CourseSectionChanged) = channel.emit(event)
    suspend fun send(event: CourseCompletionSet) = channel.emit(event)
    suspend fun send(event: CalendarSyncEvent) = channel.emit(event)
    suspend fun send(event: CourseDatesShifted) = channel.emit(event)
    suspend fun send(event: CourseLoading) = channel.emit(event)
    suspend fun send(event: CourseOpenBlock) = channel.emit(event)
    suspend fun send(event: RefreshDates) = channel.emit(event)
    suspend fun send(event: RefreshDiscussions) = channel.emit(event)
    suspend fun send(event: RefreshProgress) = channel.emit(event)
}
