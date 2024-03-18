package org.openedx.core.module.download

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.BlockType
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.CoreAnalyticsEvent
import org.openedx.core.presentation.CoreAnalyticsKey
import org.openedx.core.presentation.CoreAnalyticsValue
import org.openedx.core.utils.Sha1Util
import java.io.File

abstract class BaseDownloadViewModel(
    private val courseId:String,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val workerController: DownloadWorkerController,
    private val analytics: CoreAnalytics,
) : BaseViewModel() {

    private val allBlocks = hashMapOf<String, Block>()

    private val downloadableChildrenMap = hashMapOf<String, List<String>>()
    private val downloadModelsStatus = hashMapOf<String, DownloadedState>()

    private val _downloadModelsStatusFlow = MutableSharedFlow<HashMap<String, DownloadedState>>()
    protected val downloadModelsStatusFlow = _downloadModelsStatusFlow.asSharedFlow()

    private var downloadingModelsList = listOf<DownloadModel>()
    private val _downloadingModelsFlow = MutableSharedFlow<List<DownloadModel>>()
    protected val downloadingModelsFlow = _downloadingModelsFlow.asSharedFlow()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            downloadDao.readAllData().map { list -> list.map { it.mapToDomain() } }
                .collect { downloadModels ->
                    updateDownloadModelsStatus(downloadModels)
                    _downloadModelsStatusFlow.emit(downloadModelsStatus)
                }
        }
    }

    protected suspend fun initDownloadModelsStatus() {
        updateDownloadModelsStatus(getDownloadModelList())
        _downloadModelsStatusFlow.emit(downloadModelsStatus)
    }

    private suspend fun getDownloadModelList(): List<DownloadModel> {
        return downloadDao.readAllData().first().map { it.mapToDomain() }
    }

    private suspend fun updateDownloadModelsStatus(models: List<DownloadModel>) {
        val downloadModelMap = models.associateBy { it.id }
        for (item in downloadableChildrenMap) {
            var downloadingCount = 0
            var downloadedCount = 0
            item.value.forEach { blockId ->
                val downloadModel = downloadModelMap[blockId]
                if (downloadModel != null) {
                    if (downloadModel.downloadedState.isWaitingOrDownloading) {
                        downloadModelsStatus[blockId] = DownloadedState.DOWNLOADING
                        downloadingCount++
                    } else if (downloadModel.downloadedState.isDownloaded) {
                        downloadModelsStatus[blockId] = DownloadedState.DOWNLOADED
                        downloadedCount++
                    }
                } else {
                    downloadModelsStatus[blockId] = DownloadedState.NOT_DOWNLOADED
                }
            }

            downloadModelsStatus[item.key] = when {
                downloadingCount > 0 -> DownloadedState.DOWNLOADING
                downloadedCount == item.value.size -> DownloadedState.DOWNLOADED
                else -> DownloadedState.NOT_DOWNLOADED
            }
        }

        downloadingModelsList = models.filter { it.downloadedState.isWaitingOrDownloading }
        _downloadingModelsFlow.emit(downloadingModelsList)
    }

    protected fun setBlocks(list: List<Block>) {
        downloadableChildrenMap.clear()
        allBlocks.clear()
        allBlocks.putAll(list.map { it.id to it })
    }

    fun isBlockDownloading(id: String): Boolean {
        val blockDownloadingState = downloadModelsStatus[id]
        return blockDownloadingState?.isWaitingOrDownloading == true
    }

    fun isBlockDownloaded(id: String): Boolean {
        val blockDownloadingState = downloadModelsStatus[id]
        return blockDownloadingState == DownloadedState.DOWNLOADED
    }

    open fun saveDownloadModels(folder: String, id: String) {
        viewModelScope.launch {
            val saveBlocksIds = downloadableChildrenMap[id] ?: listOf()
            saveDownloadModels(folder, saveBlocksIds)
        }
    }

    open fun saveAllDownloadModels(folder: String) {
        viewModelScope.launch {
            val saveBlocksIds = downloadableChildrenMap.values.flatten()
            saveDownloadModels(folder, saveBlocksIds)
        }
    }

    private suspend fun saveDownloadModels(folder: String, saveBlocksIds: List<String>) {
        val downloadModels = mutableListOf<DownloadModel>()
        val downloadModelList = getDownloadModelList()
        for (blockId in saveBlocksIds) {
            allBlocks[blockId]?.let { block ->
                val videoInfo =
                    block.studentViewData?.encodedVideos?.getPreferredVideoInfoForDownloading(
                        preferencesManager.videoSettings.videoDownloadQuality
                    )
                val size = videoInfo?.fileSize ?: 0
                val url = videoInfo?.url ?: ""
                val extension = url.split('.').lastOrNull() ?: "mp4"
                val path =
                    folder + File.separator + "${Sha1Util.SHA1(block.displayName)}.$extension"
                if (downloadModelList.find { it.id == blockId && it.downloadedState.isDownloaded } == null) {
                    downloadModels.add(
                        DownloadModel(
                            block.id,
                            block.displayName,
                            size,
                            path,
                            url,
                            block.downloadableType,
                            DownloadedState.WAITING,
                            null
                        )
                    )
                }
            }
        }
        workerController.saveModels(downloadModels)
    }

    fun getDownloadModelsStatus() = downloadModelsStatus.toMap()

    fun getDownloadModelsSize(): DownloadModelsSize {
        var isAllBlocksDownloadedOrDownloading = true
        var remainingCount = 0
        var remainingSize = 0L
        var allCount = 0
        var allSize = 0L

        downloadableChildrenMap.keys.forEach { id ->
            if (!isBlockDownloaded(id) && !isBlockDownloading(id)) {
                isAllBlocksDownloadedOrDownloading = false
            }

            downloadableChildrenMap[id]?.forEach { downloadableBlock ->
                val block = allBlocks[downloadableBlock]
                val videoInfo =
                    block?.studentViewData?.encodedVideos?.getPreferredVideoInfoForDownloading(
                        preferencesManager.videoSettings.videoDownloadQuality
                    )

                allCount++
                allSize += videoInfo?.fileSize ?: 0

                if (!isBlockDownloaded(downloadableBlock)) {
                    remainingCount++
                    remainingSize += videoInfo?.fileSize ?: 0
                }
            }
        }
        return DownloadModelsSize(
            isAllBlocksDownloadedOrDownloading = isAllBlocksDownloadedOrDownloading,
            remainingCount = remainingCount,
            remainingSize = remainingSize,
            allCount = allCount,
            allSize = allSize
        )
    }

    fun hasDownloadModelsInQueue() = downloadingModelsList.isNotEmpty()

    fun getDownloadableChildren(id: String) = downloadableChildrenMap[id]

    open fun removeDownloadModels(blockId: String) {
        logSubsectionDeleteEvent(blockId)
        viewModelScope.launch {
            val downloadableChildren = downloadableChildrenMap[blockId] ?: listOf()
            workerController.removeModels(downloadableChildren)
        }
    }

    fun removeAllDownloadModels() {
        viewModelScope.launch {
            val downloadableChildren = downloadableChildrenMap.values.flatten()
            workerController.removeModels(downloadableChildren)
        }
    }

    protected fun addDownloadableChildrenForSequentialBlock(sequentialBlock: Block) {
        for (item in sequentialBlock.descendants) {
            allBlocks[item]?.let { blockDescendant ->
                if (blockDescendant.type == BlockType.VERTICAL) {
                    for (unitBlockId in blockDescendant.descendants) {
                        val block = allBlocks[unitBlockId]
                        if (block?.isDownloadable == true) {
                            val id = sequentialBlock.id
                            val children = downloadableChildrenMap[id] ?: listOf()
                            downloadableChildrenMap[id] = children + block.id
                        }
                    }
                }
            }
        }
    }

    protected fun addDownloadableChildrenForVerticalBlock(verticalBlock: Block) {
        for (unitBlockId in verticalBlock.descendants) {
            val block = allBlocks[unitBlockId]
            if (block?.isDownloadable == true) {
                val id = verticalBlock.id
                val children = downloadableChildrenMap[id] ?: listOf()
                downloadableChildrenMap[id] = children + block.id
            }
        }
    }

    fun logBulkDownloadToggleEvent(toggle: Boolean) {
        logEvent(CoreAnalyticsEvent.VIDEO_BULK_DOWNLOAD_TOGGLE.event,
            buildMap {
                put(
                    CoreAnalyticsKey.NAME.key,
                    CoreAnalyticsValue.VIDEO_BULK_DOWNLOAD_TOGGLE.biValue
                )
                put(CoreAnalyticsKey.ACTION.key, toggle.toString())
            }
        )
    }

    fun logSubsectionDownloadEvent(subsectionId: String) {
        logEvent(CoreAnalyticsEvent.VIDEO_DOWNLOAD_SUBSECTION.event,
            buildMap {
                put(
                    CoreAnalyticsKey.NAME.key,
                    CoreAnalyticsValue.VIDEO_DOWNLOAD_SUBSECTION.biValue
                )
                put(CoreAnalyticsKey.COURSE_ID.key, courseId)
                put(CoreAnalyticsKey.BLOCK_ID.key, subsectionId)
            }
        )
    }

    fun logSubsectionDeleteEvent(subsectionId: String) {
        logEvent(CoreAnalyticsEvent.VIDEO_DELETE_SUBSECTION.event,
            buildMap {
                put(
                    CoreAnalyticsKey.NAME.key,
                    CoreAnalyticsValue.VIDEO_DELETE_SUBSECTION.biValue
                )
                put(CoreAnalyticsKey.COURSE_ID.key, courseId)
                put(CoreAnalyticsKey.BLOCK_ID.key, subsectionId)
            }
        )
    }

    private fun logEvent(event: String, params: Map<String, Any?>) {
        analytics.logEvent(event, params)
    }
}
