package org.openedx.app.system.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.app.data.api.NotificationsApi
import org.openedx.core.data.storage.CorePreferences

class SyncFirebaseTokenWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {

    private val preferences: CorePreferences by inject()
    private val api: NotificationsApi by inject()

    override suspend fun doWork(): Result {
        if (preferences.user != null && preferences.pushToken.isNotEmpty()) {
            api.syncFirebaseToken(preferences.pushToken)

            return Result.success()
        }
        return Result.failure()
    }

    companion object {
        private const val WORKER_TAG = "SyncFirebaseTokenWorker"

        fun schedule(context: Context) {
            val work = OneTimeWorkRequest
                .Builder(SyncFirebaseTokenWorker::class.java)
                .addTag(WORKER_TAG)
                .build()
            WorkManager.getInstance(context).beginUniqueWork(
                WORKER_TAG,
                ExistingWorkPolicy.REPLACE,
                work
            ).enqueue()
        }
    }
}
