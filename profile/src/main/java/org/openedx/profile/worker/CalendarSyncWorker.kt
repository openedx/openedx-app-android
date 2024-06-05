package org.openedx.profile.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.data.model.CourseDates
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.EnrollmentStatus
import org.openedx.profile.system.CalendarManager
import org.openedx.profile.system.notifier.CalendarNotifier
import org.openedx.profile.system.notifier.CalendarSyncFailed
import org.openedx.profile.system.notifier.CalendarSynced
import org.openedx.profile.system.notifier.CalendarSyncing

class CalendarSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val calendarManager: CalendarManager by inject()
    private val calendarInteractor: CalendarInteractor by inject()
    private val calendarNotifier: CalendarNotifier by inject()
    private val calendarPreferences: CalendarPreferences by inject()

    override suspend fun doWork(): Result {
        return try {
            val courseId = inputData.getString(ARG_COURSE_ID)
            tryToSyncCalendar(courseId)
            Result.success()
        } catch (e: Exception) {
            calendarNotifier.send(CalendarSyncFailed)
            Result.failure()
        }
    }

    private suspend fun tryToSyncCalendar(courseId: String?) {
        val isCalendarCreated = calendarPreferences.calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST
        val isCalendarSyncEnabled = calendarPreferences.isCalendarSyncEnabled
        if (isCalendarCreated && isCalendarSyncEnabled) {
            calendarNotifier.send(CalendarSyncing)
            if (courseId.isNullOrEmpty()) {
                syncCalendar()
            } else {
                syncCalendar(courseId)
            }
            calendarNotifier.send(CalendarSynced)
        }
    }

    private suspend fun syncCalendar(courseId: String) {
        calendarInteractor.getEnrollmentsStatus()
            .find { it.courseId == courseId }
            ?.let { enrollmentStatus ->
                syncCourseEvents(enrollmentStatus)
            }
    }

    private suspend fun syncCalendar() {
        calendarInteractor.getEnrollmentsStatus()
            .forEach { enrollmentStatus ->
                syncCourseEvents(enrollmentStatus)
            }
    }

    private suspend fun syncCourseEvents(enrollmentStatus: EnrollmentStatus) {
        createCalendarState(enrollmentStatus)
        val courseId = enrollmentStatus.courseId
        if (enrollmentStatus.isActive && isCourseSyncEnabled(courseId)) {
            val courseDates = calendarInteractor.getCourseDates(courseId)
            val isCourseCalendarUpToDate = isCourseCalendarUpToDate(courseId, courseDates)
            if (!isCourseCalendarUpToDate) {
                removeCalendarEvents(courseId)
                updateCourseEvents(courseDates, enrollmentStatus)
            }
        } else {
            removeCalendarEvents(courseId)
        }
    }

    private suspend fun updateCourseEvents(courseDates: CourseDates, enrollmentStatus: EnrollmentStatus) {
        courseDates.courseDateBlocks.forEach { courseDateBlock ->
            courseDateBlock.mapToDomain()?.let { domainCourseDateBlock ->
                createEvent(domainCourseDateBlock, enrollmentStatus)
            }
        }
        calendarInteractor.updateCourseCalendarStateById(
            courseId = enrollmentStatus.courseId,
            checksum = getCourseChecksum(courseDates)
        )
    }

    private suspend fun removeCalendarEvents(courseId: String) {
        calendarInteractor.getCourseCalendarEventsById(courseId).forEach {
            calendarManager.deleteEvent(it.eventId)
        }
        calendarInteractor.deleteCourseCalendarEntitiesById(courseId)
        calendarInteractor.updateCourseCalendarStateById(courseId = courseId, checksum = 0)
    }

    private suspend fun createEvent(courseDateBlock: CourseDateBlock, enrollmentStatus: EnrollmentStatus) {
        val eventId = calendarManager.addEventsIntoCalendar(
            calendarId = calendarPreferences.calendarId,
            courseId = enrollmentStatus.courseId,
            courseName = enrollmentStatus.courseName,
            courseDateBlock = courseDateBlock
        )
        val courseCalendarEventEntity = CourseCalendarEventEntity(
            courseId = enrollmentStatus.courseId,
            eventId = eventId
        )
        calendarInteractor.insertCourseCalendarEntity(courseCalendarEventEntity)
    }

    private suspend fun createCalendarState(enrollmentStatus: EnrollmentStatus) {
        val courseCalendarStateChecksum = getCourseCalendarStateChecksum(enrollmentStatus.courseId)
        if (courseCalendarStateChecksum == null) {
            val courseCalendarStateEntity = CourseCalendarStateEntity(
                courseId = enrollmentStatus.courseId,
                isCourseSyncEnabled = enrollmentStatus.isActive
            )
            calendarInteractor.insertCourseCalendarStateEntity(courseCalendarStateEntity)
        }
    }

    private suspend fun isCourseCalendarUpToDate(courseId: String, courseDates: CourseDates): Boolean {
        val oldChecksum = getCourseCalendarStateChecksum(courseId)
        val newChecksum = getCourseChecksum(courseDates)
        return newChecksum == oldChecksum
    }

    private suspend fun isCourseSyncEnabled(courseId: String): Boolean {
        return calendarInteractor.getCourseCalendarStateById(courseId)?.mapToDomain()?.isCourseSyncEnabled ?: true
    }

    private fun getCourseChecksum(courseDates: CourseDates): Int {
        return courseDates.courseDateBlocks.sumOf { it.mapToDomain().hashCode() }
    }

    private suspend fun getCourseCalendarStateChecksum(courseId: String): Int? {
        return calendarInteractor.getCourseCalendarStateById(courseId)?.mapToDomain()?.checksum
    }

    companion object {
        const val ARG_COURSE_ID = "ARG_COURSE_ID"
        const val WORKER_TAG = "calendar_sync_worker_tag"
    }
}
