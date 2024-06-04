package org.openedx.profile.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.profile.system.CalendarManager
import org.openedx.profile.system.notifier.CalendarNotifier
import org.openedx.profile.system.notifier.CalendarSyncFailed
import org.openedx.profile.system.notifier.CalendarSynced
import org.openedx.profile.system.notifier.CalendarSyncing
import java.util.Date

class CalendarSyncService : Service() {

    private val calendarManager: CalendarManager by inject()
    private val calendarInteractor: CalendarInteractor by inject()
    private val calendarNotifier: CalendarNotifier by inject()
    private val calendarPreferences: CalendarPreferences by inject()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        syncCalendar()
        return START_STICKY
    }

    private fun syncCalendar() {
        CoroutineScope(Dispatchers.IO).launch {
            calendarNotifier.send(CalendarSyncing)
            try {
                val courseDatesDeferredResults = calendarInteractor.getEnrollmentsStatus()
                    .filter { it.isActive }
                    .map { enrollmentStatus ->
                        async(Dispatchers.IO) {
                            Pair(enrollmentStatus, calendarInteractor.getCourseDates(enrollmentStatus.courseId))
                        }
                    }
                val calendarSyncDeferred =
                    courseDatesDeferredResults.awaitAll().map { (enrollmentStatus, courseDatesResult) ->
                        async {
                            courseDatesResult.courseDateBlocks.forEach { courseDateBlock ->
                                courseDateBlock.mapToDomain()?.let { domainCourseDateBlock ->
                                    val eventId = calendarManager.addEventsIntoCalendar(
                                        calendarId = calendarPreferences.calendarId,
                                        courseId = enrollmentStatus.courseId,
                                        courseName = enrollmentStatus.courseName,
                                        courseDateBlock = domainCourseDateBlock
                                    )
                                    courseDateBlock.mapToRoomEntity()?.let { courseDateBlockDb ->
                                        val courseCalendarEventEntity = CourseCalendarEventEntity(
                                            courseId = enrollmentStatus.courseId,
                                            eventId = eventId,
                                            courseDateBlockDb = courseDateBlockDb
                                        )
                                        calendarInteractor.insertCourseCalendarEntity(courseCalendarEventEntity)
                                    }
                                }
                            }
                        }
                    }
                calendarSyncDeferred.awaitAll()
                calendarPreferences.lastCalendarSync = Date().time
                calendarNotifier.send(CalendarSynced)
                Log.e("___", "${calendarInteractor.getCourseCalendarEventEntityFromCache()}")
            } catch (e: Exception) {
                Log.e("___", "$e")
                calendarNotifier.send(CalendarSyncFailed)
            }
        }
    }
}
