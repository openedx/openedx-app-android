package org.openedx.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.R
import org.openedx.core.data.model.CourseDates
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.EnrollmentStatus
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.core.system.notifier.calendar.CalendarSyncFailed
import org.openedx.core.system.notifier.calendar.CalendarSyncOffline
import org.openedx.core.system.notifier.calendar.CalendarSynced
import org.openedx.core.system.notifier.calendar.CalendarSyncing

class CalendarSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val calendarManager: CalendarManager by inject()
    private val calendarInteractor: CalendarInteractor by inject()
    private val calendarNotifier: CalendarNotifier by inject()
    private val calendarPreferences: CalendarPreferences by inject()
    private val networkConnection: NetworkConnection by inject()

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANEL_ID)

    private val failedCoursesSync = mutableSetOf<String>()

    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            val courseId = inputData.getString(ARG_COURSE_ID)
            tryToSyncCalendar(courseId)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            calendarNotifier.send(CalendarSyncFailed)
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }

        return ForegroundInfo(
            NOTIFICATION_ID,
            notificationBuilder
                .setSmallIcon(R.drawable.core_ic_calendar)
                .setContentText(context.getString(R.string.core_title_syncing_calendar))
                .setContentTitle("")
                .build(),
            serviceType
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationChannel =
            NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                context.getString(R.string.core_header_sync_to_calendar),
                NotificationManager.IMPORTANCE_LOW
            )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private suspend fun tryToSyncCalendar(courseId: String?) {
        val isCalendarCreated = calendarPreferences.calendarId != CalendarManager.CALENDAR_DOES_NOT_EXIST
        val isCalendarSyncEnabled = calendarPreferences.isCalendarSyncEnabled
        if (!networkConnection.isOnline()) {
            calendarNotifier.send(CalendarSyncOffline)
        } else if (isCalendarCreated && isCalendarSyncEnabled) {
            calendarNotifier.send(CalendarSyncing)
            val enrollmentsStatus = calendarInteractor.getEnrollmentsStatus()
            if (courseId.isNullOrEmpty()) {
                syncCalendar(enrollmentsStatus)
            } else {
                syncCalendar(enrollmentsStatus, courseId)
            }
            removeUnenrolledCourseEvents(enrollmentsStatus)
            if (failedCoursesSync.isEmpty()) {
                calendarNotifier.send(CalendarSynced)
            } else {
                calendarNotifier.send(CalendarSyncFailed)
            }
        }
    }

    private suspend fun removeUnenrolledCourseEvents(enrollmentStatus: List<EnrollmentStatus>) {
        val enrolledCourseIds = enrollmentStatus.map { it.courseId }
        val cachedCourseIds = calendarInteractor.getAllCourseCalendarStateFromCache().map { it.courseId }
        val unenrolledCourseIds = cachedCourseIds.filter { it !in enrolledCourseIds }
        unenrolledCourseIds.forEach { courseId ->
            removeCalendarEvents(courseId)
            calendarInteractor.deleteCourseCalendarStateByIdFromCache(courseId)
        }
    }

    private suspend fun syncCalendar(enrollmentsStatus: List<EnrollmentStatus>, courseId: String) {
        enrollmentsStatus
            .find { it.courseId == courseId }
            ?.let { enrollmentStatus ->
                syncCourseEvents(enrollmentStatus)
            }
    }

    private suspend fun syncCalendar(enrollmentsStatus: List<EnrollmentStatus>) {
        enrollmentsStatus.forEach { enrollmentStatus ->
            syncCourseEvents(enrollmentStatus)
        }
    }

    private suspend fun syncCourseEvents(enrollmentStatus: EnrollmentStatus) {
        val courseId = enrollmentStatus.courseId
        try {
            createCalendarState(enrollmentStatus)
            if (enrollmentStatus.recentlyActive && isCourseSyncEnabled(courseId)) {
                val courseDates = calendarInteractor.getCourseDates(courseId)
                val isCourseCalendarUpToDate = isCourseCalendarUpToDate(courseId, courseDates)
                if (!isCourseCalendarUpToDate) {
                    removeCalendarEvents(courseId)
                    updateCourseEvents(courseDates, enrollmentStatus)
                }
            } else {
                removeCalendarEvents(courseId)
            }
        } catch (e: Exception) {
            failedCoursesSync.add(courseId)
            e.printStackTrace()
        }
    }

    private suspend fun updateCourseEvents(courseDates: CourseDates, enrollmentStatus: EnrollmentStatus) {
        courseDates.courseDateBlocks.forEach { courseDateBlock ->
            courseDateBlock.mapToDomain()?.let { domainCourseDateBlock ->
                createEvent(domainCourseDateBlock, enrollmentStatus)
            }
        }
        calendarInteractor.updateCourseCalendarStateByIdInCache(
            courseId = enrollmentStatus.courseId,
            checksum = getCourseChecksum(courseDates)
        )
    }

    private suspend fun removeCalendarEvents(courseId: String) {
        calendarInteractor.getCourseCalendarEventsByIdFromCache(courseId).forEach {
            calendarManager.deleteEvent(it.eventId)
        }
        calendarInteractor.deleteCourseCalendarEntitiesByIdFromCache(courseId)
        calendarInteractor.updateCourseCalendarStateByIdInCache(courseId = courseId, checksum = 0)
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
        calendarInteractor.insertCourseCalendarEntityToCache(courseCalendarEventEntity)
    }

    private suspend fun createCalendarState(enrollmentStatus: EnrollmentStatus) {
        val courseCalendarStateChecksum = getCourseCalendarStateChecksum(enrollmentStatus.courseId)
        if (courseCalendarStateChecksum == null) {
            val courseCalendarStateEntity = CourseCalendarStateEntity(
                courseId = enrollmentStatus.courseId,
                isCourseSyncEnabled = enrollmentStatus.recentlyActive
            )
            calendarInteractor.insertCourseCalendarStateEntityToCache(courseCalendarStateEntity)
        }
    }

    private suspend fun isCourseCalendarUpToDate(courseId: String, courseDates: CourseDates): Boolean {
        val oldChecksum = getCourseCalendarStateChecksum(courseId)
        val newChecksum = getCourseChecksum(courseDates)
        return newChecksum == oldChecksum
    }

    private suspend fun isCourseSyncEnabled(courseId: String): Boolean {
        return calendarInteractor.getCourseCalendarStateByIdFromCache(courseId)?.isCourseSyncEnabled ?: true
    }

    private fun getCourseChecksum(courseDates: CourseDates): Int {
        return courseDates.courseDateBlocks.sumOf { it.mapToDomain().hashCode() }
    }

    private suspend fun getCourseCalendarStateChecksum(courseId: String): Int? {
        return calendarInteractor.getCourseCalendarStateByIdFromCache(courseId)?.checksum
    }

    companion object {
        const val ARG_COURSE_ID = "ARG_COURSE_ID"
        const val WORKER_TAG = "calendar_sync_worker_tag"
        const val NOTIFICATION_ID = 1234
        const val NOTIFICATION_CHANEL_ID = "calendar_sync_channel"
    }
}
