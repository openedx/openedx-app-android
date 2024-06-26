package org.openedx.core.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class CalendarSyncScheduler(private val context: Context) {

    fun scheduleDailySync() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(1, TimeUnit.DAYS)
            .addTag(CalendarSyncWorker.WORKER_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CalendarSyncWorker.WORKER_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun requestImmediateSync() {
        val syncWorkRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }

    fun requestImmediateSync(courseId: String) {
        val inputData = Data.Builder()
            .putString(CalendarSyncWorker.ARG_COURSE_ID, courseId)
            .build()
        val syncWorkRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }
}
