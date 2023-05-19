package com.raccoongang.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CourseNotifier {

    private val channel = MutableSharedFlow<CourseEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<CourseEvent> = channel.asSharedFlow()

    suspend fun send(event: CourseVideoPositionChanged) = channel.emit(event)
    suspend fun send(event: CourseStructureUpdated) = channel.emit(event)
    suspend fun send(event: CourseDashboardUpdate) = channel.emit(event)
    suspend fun send(event: CoursePauseVideo) = channel.emit(event)
    suspend fun send(event: CourseSubtitleLanguageChanged) = channel.emit(event)
    suspend fun send(event: CourseSectionChanged) = channel.emit(event)

}