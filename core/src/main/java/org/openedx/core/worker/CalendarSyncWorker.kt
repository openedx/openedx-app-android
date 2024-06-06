package org.openedx.core.worker

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.openedx.core.service.CalendarSyncForegroundService

class CalendarSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    override suspend fun doWork(): Result {
        return try {
            val courseId = inputData.getString(ARG_COURSE_ID)
            startForegroundService(courseId)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun startForegroundService(courseId: String?) {
        val serviceIntent = Intent(applicationContext, CalendarSyncForegroundService::class.java).apply {
            putExtra(CalendarSyncForegroundService.ARG_COURSE_ID, courseId)
        }
        ContextCompat.startForegroundService(applicationContext, serviceIntent)
    }

    companion object {
        const val ARG_COURSE_ID = "ARG_COURSE_ID"
        const val WORKER_TAG = "calendar_sync_worker_tag"
    }
}
