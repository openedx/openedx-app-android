package org.openedx.core.module

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import org.openedx.core.R
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.download.CurrentProgress
import org.openedx.core.module.download.DownloadHelper
import org.openedx.core.module.download.FileDownloader
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.DownloadProgressChanged
import org.openedx.core.utils.FileUtil

class DownloadWorker(
    val context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters), CoroutineScope {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    private val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)

    private val notifier by inject<DownloadNotifier>(DownloadNotifier::class.java)
    private val downloadDao: DownloadDao by inject(DownloadDao::class.java)
    private val downloadHelper: DownloadHelper by inject(DownloadHelper::class.java)

    private var downloadEnqueue = listOf<DownloadModel>()

    private val folder = FileUtil(context).getExternalAppDir()

    private var currentDownload: DownloadModel? = null
    private var lastUpdateTime = 0L

    private val fileDownloader by inject<FileDownloader>(FileDownloader::class.java)

    override suspend fun doWork(): Result {
        updateProgress()

        setForeground(createForegroundInfo())
        newDownload()
        fileDownloader.progressListener = null
        return Result.success()
    }


    private fun createForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0

        return ForegroundInfo(
            NOTIFICATION_ID,
            notificationBuilder
                .setSmallIcon(R.drawable.core_ic_check_in_box)
                .setProgress(100, 0, false)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setContentText(context.getString(R.string.core_downloading_in_progress))
                .setContentTitle("")
                .build(),
            serviceType
        )
    }

    private fun updateProgress() {
        fileDownloader.progressListener = object : CurrentProgress {
            override fun progress(value: Long, size: Long) {
                val progress = 100 * value / size
                // Update no more than 5 times per sec
                if (!fileDownloader.isCanceled &&
                    (System.currentTimeMillis() - lastUpdateTime > 200)
                ) {
                    lastUpdateTime = System.currentTimeMillis()

                    currentDownload?.let {
                        launch {
                            notifier.send(DownloadProgressChanged(it.id, value, size))
                        }

                        notificationManager.notify(
                            NOTIFICATION_ID,
                            notificationBuilder
                                .setSmallIcon(R.drawable.core_ic_check_in_box)
                                .setProgress(100, progress.toInt(), false)
                                .setPriority(NotificationManager.IMPORTANCE_LOW)
                                .setContentText(context.getString(R.string.core_downloading_in_progress))
                                .setContentTitle(it.title)
                                .build()
                        )
                    }
                }
            }
        }
    }

    private suspend fun newDownload() {
        if (!folder.exists()) {
            folder.mkdir()
        }

        downloadEnqueue = downloadDao.readAllData().first()
            .map { it.mapToDomain() }
            .filter { it.downloadedState == DownloadedState.WAITING }

        val downloadTask = downloadEnqueue.getOrNull(0)

        if (downloadTask != null) {
            currentDownload = downloadTask
            downloadDao.updateDownloadModel(
                DownloadModelEntity.createFrom(
                    downloadTask.copy(
                        downloadedState = DownloadedState.DOWNLOADING
                    )
                )
            )
            val isSuccess = fileDownloader.download(downloadTask.url, downloadTask.path)
            if (isSuccess) {
                val updatedModel = downloadHelper.updateDownloadStatus(downloadTask)
                downloadDao.updateDownloadModel(
                    DownloadModelEntity.createFrom(updatedModel)
                )
            } else {
                downloadDao.removeDownloadModel(downloadTask.id)
            }
            newDownload()
        } else {
            return
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationChannel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    companion object {
        const val WORKER_TAG = "downloadWorker"

        private const val CHANNEL_ID = "download_channel_ID"
        private const val CHANNEL_NAME = "download_channel_name"
        private const val NOTIFICATION_ID = 10
    }

}
