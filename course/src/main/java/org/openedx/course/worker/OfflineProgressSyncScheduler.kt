package org.openedx.course.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class OfflineProgressSyncScheduler(private val context: Context) {

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OfflineProgressSyncWorker>()
            .addTag(OfflineProgressSyncWorker.WORKER_TAG)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.HOURS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OfflineProgressSyncWorker.WORKER_TAG,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
