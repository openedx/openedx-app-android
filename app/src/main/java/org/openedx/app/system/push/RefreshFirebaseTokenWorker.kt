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

/**
 * Rotates the device's push token after a sign-out, so a backend that missed the unregistration
 * can no longer reach this device. The token is device-wide, so this is device-level work with
 * one unique name. It rotates only when no site is still signed in: rotating would cut off the
 * sites that still use the token. Any site signed in by the time the new token arrives gets it.
 */
class RefreshFirebaseTokenWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {

    private val preferences: CorePreferences by inject()
    private val registrations: PushRegistrations by inject()

    override suspend fun doWork(): Result {
        if (registrations.signedInSites().isNotEmpty()) return Result.success()

        FirebaseMessaging.getInstance().deleteToken().await()
        val newPushToken = FirebaseMessaging.getInstance().getToken().await()
        preferences.pushToken = newPushToken

        SyncFirebaseTokenWorker.scheduleForSignedInSites(applicationContext, registrations)
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
