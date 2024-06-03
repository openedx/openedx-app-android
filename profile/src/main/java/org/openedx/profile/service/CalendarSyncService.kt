package org.openedx.profile.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
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
    private val calendarRepository: CalendarInteractor by inject()
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
                val courseDatesDeferredResults = calendarRepository.getEnrollmentsStatus()
                    .filter { it.isActive }
                    .map { enrollmentStatus ->
                        async(Dispatchers.IO) {
                            Pair(enrollmentStatus, calendarRepository.getCourseDates(enrollmentStatus.courseId))
                        }
                    }
                val courseDates = courseDatesDeferredResults.awaitAll().filterNotNull()
                val calendarSyncDeferred = courseDates.map { (enrollmentStatus, courseDatesResult) ->
                    async {
                        courseDatesResult.courseDateBlocks.forEach { courseDateBlock ->
                            courseDateBlock.mapToDomain()?.let { domainCourseDateBlock ->
                                calendarManager.addEventsIntoCalendar(
                                    calendarId = calendarPreferences.calendarId,
                                    courseId = enrollmentStatus.courseId,
                                    courseName = enrollmentStatus.courseName,
                                    courseDateBlock = domainCourseDateBlock
                                )
                            }
                        }
                    }
                }
                calendarSyncDeferred.awaitAll()
                calendarPreferences.lastCalendarSync = Date().time
                calendarNotifier.send(CalendarSynced)
            } catch (e: Exception) {
                calendarNotifier.send(CalendarSyncFailed)
            }
        }
    }
}
