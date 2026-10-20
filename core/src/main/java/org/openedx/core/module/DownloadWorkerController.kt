package org.openedx.core.module

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openedx.core.instance.StorageNamespaceProvider
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.download.FileDownloader
import java.io.File

class DownloadWorkerController(
    context: Context,
    private val downloadDao: DownloadDao,
    private val fileDownloader: FileDownloader,
    private val namespaces: StorageNamespaceProvider,
) {

    private val workManager = WorkManager.getInstance(context)
    private var downloadTaskList = listOf<DownloadModel>()

    init {
        GlobalScope.launch {
            downloadDao.getAllDataFlow().collect { list ->
                val domainList = list.map { it.mapToDomain() }
                downloadTaskList = domainList.filter {
                    it.downloadedState == DownloadedState.WAITING || it.downloadedState == DownloadedState.DOWNLOADING
                }
                domainList.find { it.downloadedState == DownloadedState.WAITING }?.let {
                    if (!isWorkScheduled(DownloadWorker.WORKER_TAG)) {
                        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                            .addTag(DownloadWorker.WORKER_TAG)
                            .setInputData(DownloadWorker.inputData(namespaces.current))
                            .build()
                        workManager.enqueue(request)
                    }
                }
            }
        }
    }

    private suspend fun updateList() {
        downloadTaskList = downloadDao.getAllDataFlow().first()
            .map { it.mapToDomain() }
            .filter {
                it.downloadedState == DownloadedState.WAITING || it.downloadedState == DownloadedState.DOWNLOADING
            }
    }

    suspend fun saveModels(downloadModels: List<DownloadModel>) {
        downloadDao.insertDownloadModel(downloadModels.map { DownloadModelEntity.createFrom(it) })
    }

    suspend fun removeModel(id: String) {
        removeModels(listOf(id))
    }

    suspend fun removeModels(ids: List<String>) {
        val downloadModels = getDownloadModelsById(ids)
        val removeIds = mutableListOf<String>()
        var hasDownloading = false

        downloadModels.forEach { downloadModel ->
            removeIds.add(downloadModel.id)
            if (downloadModel.downloadedState == DownloadedState.DOWNLOADING) {
                hasDownloading = true
            }
            try {
                File(downloadModel.path).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (hasDownloading) fileDownloader.cancelDownloading()
        downloadDao.removeAllDownloadModels(removeIds)
        downloadDao.removeOfflineXBlockProgress(removeIds)

        updateList()

        // Only when the worker was on one of these: it may be busy with another site's queue,
        // which this site has no say over.
        if (hasDownloading && downloadTaskList.isEmpty()) {
            workManager.cancelAllWorkByTag(DownloadWorker.WORKER_TAG)
        }
    }

    /**
     * Cancels every download the selected site hasn't finished: the active one and the queue.
     * Finished files stay.
     */
    suspend fun cancelUnfinishedDownloads() {
        val unfinished = downloadDao.readAllData()
            .map { it.mapToDomain() }
            .filter {
                it.downloadedState == DownloadedState.WAITING || it.downloadedState == DownloadedState.DOWNLOADING
            }
            .map { it.id }
        if (unfinished.isNotEmpty()) removeModels(unfinished)
    }

    suspend fun removeModels() {
        fileDownloader.cancelDownloading()
        workManager.cancelAllWorkByTag(DownloadWorker.WORKER_TAG)
    }

    private fun isWorkScheduled(tag: String): Boolean {
        val statuses = workManager.getWorkInfosByTag(tag)
        return try {
            val workInfo = statuses.get().find {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            }
            workInfo != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun getDownloadModelsById(ids: List<String>): List<DownloadModel> {
        return downloadDao.readAllDataByIds(ids).first().map { it.mapToDomain() }
    }
}
