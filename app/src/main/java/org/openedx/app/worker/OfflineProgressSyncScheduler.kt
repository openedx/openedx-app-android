package org.openedx.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class OfflineProgressSyncScheduler(private val context: Context) {

    fun scheduleHourlySync() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<OfflineProgressSyncWorker>(1, TimeUnit.HOURS)
            .addTag(OfflineProgressSyncWorker.WORKER_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OfflineProgressSyncWorker.WORKER_TAG,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )
    }
}
