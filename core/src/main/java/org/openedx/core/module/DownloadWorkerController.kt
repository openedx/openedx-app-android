package org.openedx.core.module

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.download.FileDownloader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException

class DownloadWorkerController(
    context: Context,
    private val downloadDao: DownloadDao,
    private val fileDownloader: FileDownloader
) {

    private val workManager = WorkManager.getInstance(context)

    private var downloadTaskList = listOf<DownloadModel>()

    init {
        GlobalScope.launch {
            downloadDao.readAllData().collect { list ->
                val domainList = list.map { it.mapToDomain() }
                downloadTaskList = domainList.filter {
                    it.downloadedState == DownloadedState.WAITING || it.downloadedState == DownloadedState.DOWNLOADING
                }
                domainList.find { it.downloadedState == DownloadedState.WAITING }?.let {
                    if (!isWorkScheduled(DownloadWorker.WORKER_TAG)) {
                        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                            .addTag(DownloadWorker.WORKER_TAG)
                            .build()
                        workManager.enqueue(request)
                    }
                }
            }
        }
    }

    private suspend fun updateList() {
        downloadTaskList =
            downloadDao.readAllData().first().map { it.mapToDomain() }.filter {
                it.downloadedState == DownloadedState.WAITING || it.downloadedState == DownloadedState.DOWNLOADING
            }
    }

    suspend fun saveModels(vararg downloadModel: DownloadModel) {
            downloadDao.insertDownloadModel(
                *downloadModel.map { DownloadModelEntity.createFrom(it) }.toTypedArray()
            )
    }

    suspend fun cancelWork(vararg ids: String) {
            for (id in ids.toList()) {
                updateList()
                val downloadModel = downloadTaskList.find { it.id == id }
                if (downloadTaskList.size == 1) {
                    fileDownloader.cancelDownloading()
                    downloadDao.removeDownloadModel(id)
                    workManager.cancelAllWorkByTag(DownloadWorker.WORKER_TAG)
                    return
                }
                downloadModel?.let {
                    if (it.downloadedState == DownloadedState.WAITING) {
                        downloadDao.removeDownloadModel(id)
                    } else {
                        fileDownloader.cancelDownloading()
                        downloadDao.removeDownloadModel(id)
                    }
                }
            }
    }

    suspend fun cancelWork() {
        fileDownloader.cancelDownloading()
        workManager.cancelAllWorkByTag(DownloadWorker.WORKER_TAG)
    }


    private fun isWorkScheduled(tag: String): Boolean {
        val statuses: ListenableFuture<List<WorkInfo>> = workManager.getWorkInfosByTag(tag)
        return try {
            val workInfoList: List<WorkInfo> = statuses.get()
            val workInfo = workInfoList.find {
                (it.state == WorkInfo.State.RUNNING) or (it.state == WorkInfo.State.ENQUEUED)
            }
            workInfo != null
        } catch (e: ExecutionException) {
            e.printStackTrace()
            false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }

}