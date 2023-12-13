package org.openedx.course.presentation.videos

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.R
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics

class CourseVideoViewModel(
    val courseId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController
) : BaseDownloadViewModel(downloadDao, preferencesManager, workerController) {

    val apiHostUrl get() = config.getApiHostURL()

    val isCourseNestedListEnabled get() = config.isCourseNestedListEnabled()

    val isCourseBannerEnabled get() = config.isCourseBannerEnabled()

    private val _uiState = MutableLiveData<CourseVideosUIState?>()
    val uiState: LiveData<CourseVideosUIState?>
        get() = _uiState

    var courseTitle = ""

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val courseSections = mutableMapOf<String, MutableList<Block>>()
    private val courseSectionsState = mutableMapOf<String, Boolean>()
    private val downloadsCount = mutableMapOf<String, Int>()
    val courseSubSection = mutableMapOf<String, Block?>()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseStructureUpdated) {
                    if (event.courseId == courseId) {
                        updateVideos()
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseVideosUIState.CourseData) {
                    val state = _uiState.value as CourseVideosUIState.CourseData
                    _uiState.value = CourseVideosUIState.CourseData(
                        courseStructure = state.courseStructure,
                        downloadedState = it.toMap(),
                        courseSections = courseSections,
                        courseSectionsState = courseSectionsState,
                        downloadsCount = downloadsCount
                    )
                }
            }
        }
    }

    init {
        getVideos()
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                _uiMessage.value =
                    UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi))
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    fun setIsUpdating() {
        _isUpdating.value = true
    }

    private fun updateVideos() {
        getVideos()
        _isUpdating.value = false
    }

    fun getVideos() {
        viewModelScope.launch {
            var courseStructure = interactor.getCourseStructureForVideos()
            val blocks = courseStructure.blockData
            if (blocks.isEmpty()) {
                _uiState.value = CourseVideosUIState.Empty(
                    message = resourceManager.getString(R.string.course_does_not_include_videos)
                )
            } else {
                setBlocks(courseStructure.blockData)
                courseSections.clear()
                courseSubSection.clear()
                courseStructure = courseStructure.copy(blockData = sortBlocks(blocks))
                initDownloadModelsStatus()
                _uiState.value =
                    CourseVideosUIState.CourseData(
                        courseStructure, getDownloadModelsStatus(), courseSections,
                        courseSectionsState, downloadsCount
                    )
            }
        }
    }

    fun switchCourseSections(blockId: String) {
        courseSectionsState[blockId] = !(courseSectionsState[blockId] ?: false)
        if (_uiState.value is CourseVideosUIState.CourseData) {
            val state = _uiState.value as CourseVideosUIState.CourseData
            _uiState.value = null
            _uiState.value = CourseVideosUIState.CourseData(
                courseStructure = state.courseStructure,
                downloadedState = state.downloadedState,
                courseSections = courseSections,
                courseSectionsState = courseSectionsState,
                downloadsCount = downloadsCount
            )
        }
    }

    fun verticalClickedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseVideosUIState.CourseData) {
            analytics.verticalClickedEvent(courseId, courseTitle, blockId, blockName)
        }
    }

    private fun sortBlocks(blocks: List<Block>): List<Block> {
        val resultBlocks = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        blocks.forEach { block ->
            if (block.type == BlockType.CHAPTER) {
                resultBlocks.add(block)
                block.descendants.forEach { descendant ->
                    blocks.find { it.id == descendant }?.let {
                        if (isCourseNestedListEnabled) {
                            courseSections.getOrPut(block.id) { mutableListOf() }
                                .add(it)
                            courseSubSection[it.id] = getCourseFirstSubSection(blocks, it)
                            downloadsCount[it.id] = getDownloadsCount(blocks, it)

                        } else {
                            resultBlocks.add(it)
                        }
                        addDownloadableChildrenForSequentialBlock(it)
                    }
                }
            }
        }
        return resultBlocks.toList()
    }

    private fun getCourseFirstSubSection(blocks: List<Block>, selectedBlock: Block): Block? {
        if (blocks.isEmpty()) return null
        selectedBlock.descendants.forEach { descendant ->
            blocks.find { it.id == descendant }?.let { block ->
                return block
            }
        }
        return null
    }

    private fun getDownloadsCount(blocks: List<Block>, selectedBlock: Block): Int {
        if (blocks.isEmpty()) return 0
        var count = 0
        selectedBlock.descendants.forEach { id ->
            blocks.find { it.id == id }?.let { descendantBlock ->
                count += blocks.filter { descendantBlock.descendants.contains(it.id) && it.isDownloadable }.size
            }
        }
        return count
    }
}