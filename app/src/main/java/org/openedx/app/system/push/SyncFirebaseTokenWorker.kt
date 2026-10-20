package org.openedx.app.system.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Registers the device's push token with one site, the one it was scheduled for. */
class SyncFirebaseTokenWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {

    private val registrations: PushRegistrations by inject()

    override suspend fun doWork(): Result {
        val instanceKey = inputData.getString(KEY_INSTANCE_KEY)
        registrations.register(instanceKey)
        return Result.success()
    }

    companion object {
        private const val WORKER_TAG = "SyncFirebaseTokenWorker"
        private const val KEY_INSTANCE_KEY = "instance_key"

        /** One unique work per site: a sync for one site never replaces another site's. */
        fun schedule(context: Context, instanceKey: String?) {
            val work = OneTimeWorkRequest
                .Builder(SyncFirebaseTokenWorker::class.java)
                .addTag(WORKER_TAG)
                .setInputData(workDataOf(KEY_INSTANCE_KEY to instanceKey))
                .build()
            WorkManager.getInstance(context).beginUniqueWork(
                "$WORKER_TAG:${instanceKey.orEmpty()}",
                ExistingWorkPolicy.REPLACE,
                work
            ).enqueue()
        }

        fun scheduleForSignedInSites(context: Context, registrations: PushRegistrations) {
            registrations.signedInSites().forEach { schedule(context, it) }
        }
    }
}
