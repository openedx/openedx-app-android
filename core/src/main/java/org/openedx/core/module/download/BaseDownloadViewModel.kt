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
import org.openedx.core.utils.Sha1Util
import java.io.File

abstract class BaseDownloadViewModel(
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val workerController: DownloadWorkerController
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
        for (item in downloadableChildrenMap) {
            if (models.find { item.value.contains(it.id) && it.downloadedState.isWaitingOrDownloading } != null) {
                downloadModelsStatus[item.key] = DownloadedState.DOWNLOADING
            } else if (item.value.all { id -> models.find { it.id == id && it.downloadedState == DownloadedState.DOWNLOADED } != null }) {
                downloadModelsStatus[item.key] = DownloadedState.DOWNLOADED
            } else {
                downloadModelsStatus[item.key] = DownloadedState.NOT_DOWNLOADED
            }
        }

        downloadingModelsList = models.filter {
            it.downloadedState == DownloadedState.DOWNLOADING ||
                    it.downloadedState == DownloadedState.WAITING
        }
        _downloadingModelsFlow.emit(downloadingModelsList)
    }

    protected fun setBlocks(list: List<Block>) {
        downloadableChildrenMap.clear()
        allBlocks.clear()
        allBlocks.putAll(list.map { it.id to it })
    }

    fun isBlockDownloading(id: String): Boolean {
        val blockDownloadingState = downloadModelsStatus[id]
        return blockDownloadingState == DownloadedState.DOWNLOADING || blockDownloadingState == DownloadedState.WAITING
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
        workerController.saveModels(*downloadModels.toTypedArray())
    }

    fun removeDownloadedModels(id: String) {
        viewModelScope.launch {
            val saveBlocksIds = downloadableChildrenMap[id] ?: listOf()
            val downloaded =
                getDownloadModelList().filter { saveBlocksIds.contains(it.id) && it.downloadedState.isDownloaded }
            downloaded.forEach {
                downloadDao.removeDownloadModel(it.id)
            }
        }
    }

    fun removeOrCancelAllDownloadModels() {
        viewModelScope.launch {
            downloadableChildrenMap.keys.forEach { id ->
                if (isBlockDownloading(id)) {
                    cancelWork(id)
                } else {
                    removeDownloadedModels(id)
                }
            }
        }
    }

    fun getDownloadModelsStatus() = downloadModelsStatus.toMap()

    fun getDownloadModelsSize(): DownloadModelsSize {
        var isAllBlocksDownloadedOrDownloading = true
        var remainingCount = 0
        var remainingSize = 0L
        var allCount = 0
        var allSize = 0L

        downloadableChildrenMap.keys.forEach { id ->
            val isBlockDownloaded = isBlockDownloaded(id)
            if (!isBlockDownloaded && !isBlockDownloading(id)) {
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

                if (!isBlockDownloaded) {
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

    open fun cancelWork(blockId: String) {
        viewModelScope.launch {
            val downloadableChildren = downloadableChildrenMap[blockId] ?: listOf()
            val ids = getDownloadModelList().filter {
                (it.downloadedState == DownloadedState.DOWNLOADING ||
                        it.downloadedState == DownloadedState.WAITING) && downloadableChildren.contains(
                    it.id
                )
            }.map { it.id }
            workerController.cancelWork(*ids.toTypedArray())
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

}
