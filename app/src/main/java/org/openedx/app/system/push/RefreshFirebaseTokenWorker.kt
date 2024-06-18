package org.openedx.app.system.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.data.storage.CorePreferences

class RefreshFirebaseTokenWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {

    private val preferences: CorePreferences by inject()

    override suspend fun doWork(): Result {
        FirebaseMessaging.getInstance().deleteToken().await()

        val newPushToken = FirebaseMessaging.getInstance().getToken().await()

        preferences.pushToken = newPushToken

        return Result.success()
    }

    companion object {
        private const val WORKER_TAG = "RefreshFirebaseTokenWorker"

        fun schedule(context: Context) {
            val work = OneTimeWorkRequest
                .Builder(RefreshFirebaseTokenWorker::class.java)
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
