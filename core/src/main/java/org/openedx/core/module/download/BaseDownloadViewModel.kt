package org.openedx.core.module.download

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
import org.openedx.foundation.presentation.BaseViewModel

abstract class BaseDownloadViewModel(
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val workerController: DownloadWorkerController,
    private val analytics: CoreAnalytics,
    private val downloadHelper: DownloadHelper,
) : BaseViewModel() {

    val allBlocks = hashMapOf<String, Block>()

    private val downloadableChildrenMap = hashMapOf<String, List<String>>()
    private val downloadModelsStatus = hashMapOf<String, DownloadedState>()

    private val _downloadModelsStatusFlow = MutableSharedFlow<HashMap<String, DownloadedState>>()
    protected val downloadModelsStatusFlow = _downloadModelsStatusFlow.asSharedFlow()

    private val _downloadingModelsFlow = MutableSharedFlow<List<DownloadModel>>()
    protected val downloadingModelsFlow = _downloadingModelsFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            downloadDao.getAllDataFlow().map { list -> list.map { it.mapToDomain() } }
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

    suspend fun getDownloadModelList(): List<DownloadModel> {
        return downloadDao.getAllDataFlow().first().map { it.mapToDomain() }
    }

    private suspend fun updateDownloadModelsStatus(models: List<DownloadModel>) {
        val downloadModelMap = models.associateBy { it.id }

        downloadableChildrenMap.forEach { (parentId, children) ->
            val (downloadingCount, downloadedCount) = updateChildrenStatus(children, downloadModelMap)
            updateParentStatus(parentId, children.size, downloadingCount, downloadedCount)
        }

        _downloadingModelsFlow.emit(models)
    }

    private fun updateChildrenStatus(
        children: List<String>,
        downloadModelMap: Map<String, DownloadModel>
    ): Pair<Int, Int> {
        var downloadingCount = 0
        var downloadedCount = 0

        children.forEach { blockId ->
            val downloadModel = downloadModelMap[blockId]
            downloadModelsStatus[blockId] = when {
                downloadModel?.downloadedState?.isWaitingOrDownloading == true -> {
                    downloadingCount++
                    DownloadedState.DOWNLOADING
                }

                downloadModel?.downloadedState?.isDownloaded == true -> {
                    downloadedCount++
                    DownloadedState.DOWNLOADED
                }

                else -> DownloadedState.NOT_DOWNLOADED
            }
        }

        return downloadingCount to downloadedCount
    }

    private fun updateParentStatus(
        parentId: String,
        childrenSize: Int,
        downloadingCount: Int,
        downloadedCount: Int
    ) {
        downloadModelsStatus[parentId] = when {
            downloadingCount > 0 -> DownloadedState.DOWNLOADING
            downloadedCount == childrenSize -> DownloadedState.DOWNLOADED
            else -> DownloadedState.NOT_DOWNLOADED
        }
    }

    protected fun setBlocks(list: List<Block>) {
        downloadableChildrenMap.clear()
        allBlocks.clear()
        allBlocks.putAll(list.map { it.id to it })
    }

    protected fun addBlocks(list: List<Block>) {
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

    open fun saveDownloadModels(folder: String, courseId: String, id: String) {
        viewModelScope.launch {
            val saveBlocksIds = downloadableChildrenMap[id] ?: listOf()
            logSubsectionDownloadEvent(id, saveBlocksIds.size, courseId)
            saveDownloadModels(folder, courseId, saveBlocksIds)
        }
    }

    open fun saveAllDownloadModels(folder: String, courseId: String) {
        viewModelScope.launch {
            val saveBlocksIds = downloadableChildrenMap.values.flatten()
            saveDownloadModels(folder, courseId, saveBlocksIds)
        }
    }

    suspend fun saveDownloadModels(folder: String, courseId: String, saveBlocksIds: List<String>) {
        val downloadModels = mutableListOf<DownloadModel>()
        val downloadModelList = getDownloadModelList()
        for (blockId in saveBlocksIds) {
            allBlocks[blockId]?.let { block ->
                val downloadModel = downloadHelper.generateDownloadModelFromBlock(folder, block, courseId)
                val isNotDownloaded =
                    downloadModelList.find { it.id == blockId && it.downloadedState.isDownloaded } == null
                if (isNotDownloaded && downloadModel != null) {
                    downloadModels.add(downloadModel)
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

    fun getDownloadableChildren(id: String) = downloadableChildrenMap[id]

    open fun removeDownloadModels(blockId: String, courseId: String) {
        viewModelScope.launch {
            val downloadableChildren = downloadableChildrenMap[blockId] ?: listOf()
            logSubsectionDeleteEvent(blockId, downloadableChildren.size, courseId)
            workerController.removeModels(downloadableChildren)
        }
    }

    fun removeBlockDownloadModel(blockId: String) {
        viewModelScope.launch {
            workerController.removeModel(blockId)
        }
    }

    @Suppress("NestedBlockDepth")
    protected fun addDownloadableChildrenForSequentialBlock(sequentialBlock: Block) {
        sequentialBlock.descendants.forEach { descendantId ->
            val blockDescendant = allBlocks[descendantId] ?: return@forEach

            if (blockDescendant.type == BlockType.VERTICAL) {
                blockDescendant.descendants.forEach { unitBlockId ->
                    val block = allBlocks[unitBlockId]
                    if (block?.isDownloadable == true) {
                        addDownloadableChild(sequentialBlock.id, block.id)
                    }
                }
            }
        }
    }

    private fun addDownloadableChild(parentId: String, childId: String) {
        val children = downloadableChildrenMap[parentId] ?: listOf()
        downloadableChildrenMap[parentId] = children + childId
    }

    private fun logSubsectionDownloadEvent(
        subsectionId: String,
        numberOfVideos: Int,
        courseId: String
    ) {
        logEvent(
            CoreAnalyticsEvent.VIDEO_DOWNLOAD_SUBSECTION,
            buildMap {
                put(CoreAnalyticsKey.BLOCK_ID.key, subsectionId)
                put(CoreAnalyticsKey.NUMBER_OF_VIDEOS.key, numberOfVideos)
            },
            courseId
        )
    }

    private fun logSubsectionDeleteEvent(
        subsectionId: String,
        numberOfVideos: Int,
        courseId: String
    ) {
        logEvent(
            CoreAnalyticsEvent.VIDEO_DELETE_SUBSECTION,
            buildMap {
                put(CoreAnalyticsKey.BLOCK_ID.key, subsectionId)
                put(CoreAnalyticsKey.NUMBER_OF_VIDEOS.key, numberOfVideos)
            },
            courseId
        )
    }

    private fun logEvent(
        event: CoreAnalyticsEvent,
        param: Map<String, Any?> = emptyMap(),
        courseId: String
    ) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(CoreAnalyticsKey.NAME.key, event.biValue)
                put(CoreAnalyticsKey.CATEGORY.key, CoreAnalyticsKey.VIDEOS.key)
                put(CoreAnalyticsKey.COURSE_ID.key, courseId)
                putAll(param)
            }
        )
    }
}
