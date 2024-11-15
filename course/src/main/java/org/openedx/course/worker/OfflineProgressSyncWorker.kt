package org.openedx.course.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.R
import org.openedx.course.domain.interactor.CourseInteractor

class OfflineProgressSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val courseInteractor: CourseInteractor by inject()

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANEL_ID)

    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            tryToSyncProgress()
            Result.success()
        } catch (e: Exception) {
            Log.e(WORKER_TAG, "$e")
            Firebase.crashlytics.log("$e")
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }

        return ForegroundInfo(
            NOTIFICATION_ID,
            notificationBuilder
                .setSmallIcon(R.drawable.core_ic_offline)
                .setContentText(context.getString(R.string.core_title_syncing_calendar))
                .setContentTitle("")
                .build(),
            serviceType
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationChannel =
            NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                context.getString(R.string.core_offline_progress_sync),
                NotificationManager.IMPORTANCE_LOW
            )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private suspend fun tryToSyncProgress() {
        courseInteractor.submitAllOfflineXBlockProgress()
    }

    companion object {
        const val WORKER_TAG = "progress_sync_worker_tag"
        const val NOTIFICATION_ID = 5678
        const val NOTIFICATION_CHANEL_ID = "progress_sync_channel"
    }
}
