package com.raccoongang.course.presentation.units

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.module.DownloadWorkerController
import com.raccoongang.core.module.db.DownloadModel
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.utils.Sha1Util
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import com.raccoongang.course.R as courseR

class CourseUnitsViewModel(
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val preferencesManager: PreferencesManager,
    private val networkConnection: NetworkConnection,
    private val workerController: DownloadWorkerController
) : BaseViewModel() {

    private val _uiState = MutableLiveData<CourseUnitsUIState>(CourseUnitsUIState.Loading)
    val uiState: LiveData<CourseUnitsUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            getDownloadModels().collect { list ->
                if (uiState.value is CourseUnitsUIState.Blocks) {
                    val uiList = (uiState.value as CourseUnitsUIState.Blocks).blocks
                    _uiState.value = CourseUnitsUIState.Blocks(uiList.map { block ->
                        val downloadingBlock =
                            getDownloadModels().first().find { it.id == block.id }
                        block.copy(downloadModel = downloadingBlock)
                    })
                }
            }
        }
    }

    fun getBlocks(blockId: String, mode: CourseViewMode) {
        _uiState.value = CourseUnitsUIState.Loading
        viewModelScope.launch {
            try {
                val blocks = when (mode) {
                    CourseViewMode.FULL -> interactor.getCourseStructureFromCache()
                    CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos()
                }
                val newList = getDescendantBlocks(blocks, blockId)
                _uiState.value = CourseUnitsUIState.Blocks(newList.map { block ->
                    val downloadingBlock = getDownloadModels().first().find { it.id == block.id }
                    block.copy(downloadModel = downloadingBlock)
                })
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun cancelWork(id: String) {
        viewModelScope.launch {
            workerController.cancelWork(id)
        }
    }

    fun removeDownloadedModel(id: String) {
        viewModelScope.launch {
            interactor.removeDownloadModel(id)
        }
    }

    fun saveDownloadModel(folder: String, block: Block) {
        val videoInfo = block.studentViewData?.encodedVideos?.getPreferredVideoInfoForDownloading(
            preferencesManager.videoSettings.videoQuality
        )
        val size = videoInfo?.fileSize ?: 0
        val url = videoInfo?.url ?: ""
        val extension = url.split('.').lastOrNull() ?: "mp4"
        val path =
            folder + File.separator + "${Sha1Util.SHA1(block.displayName)}.$extension"
        val downloadModel =
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
        viewModelScope.launch {
            if (preferencesManager.videoSettings.wifiDownloadOnly) {
                if (networkConnection.isWifiConnected()) {
                    workerController.saveModels(downloadModel)
                } else {
                    _uiMessage.value =
                        UIMessage.ToastMessage(resourceManager.getString(courseR.string.course_can_download_only_with_wifi))
                }
            } else {
                workerController.saveModels(downloadModel)
            }
        }
    }

    private fun getDownloadModels() = interactor.getDownloadModels()

    private fun getDescendantBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = blocks.first {
            it.id == id
        }
        selectedBlock.descendants.forEach { descendantId ->
            blocks.firstOrNull { b -> b.id == descendantId }?.let {
                resultList.add(it)
            }
        }
        return resultList
    }

}