package org.openedx.course.presentation.unit.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openedx.core.BlockType
import org.openedx.core.config.Config
import org.openedx.core.domain.model.Block
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSectionChanged
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.foundation.extension.clearAndAddAll
import org.openedx.foundation.extension.indexOfFirstFromIndex
import org.openedx.foundation.presentation.BaseViewModel

class CourseUnitContainerViewModel(
    val courseId: String,
    val unitId: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
    private val networkConnection: NetworkConnection,
) : BaseViewModel() {

    private val blocks = ArrayList<Block>()

    val isCourseExpandableSectionsEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    val isCourseUnitProgressEnabled get() = config.getCourseUIConfig().isCourseUnitProgressEnabled

    private var currentIndex = 0
    private var currentVerticalIndex = 0
    private var currentSectionIndex = -1

    val isFirstIndexInContainer: Boolean
        get() {
            return _descendantsBlocks.value.firstOrNull() == _descendantsBlocks.value.getOrNull(currentIndex)
        }

    val isLastIndexInContainer: Boolean
        get() {
            return _descendantsBlocks.value.lastOrNull() == _descendantsBlocks.value.getOrNull(currentIndex)
        }

    private val _verticalBlockCounts = MutableLiveData<Int>()
    val verticalBlockCounts: LiveData<Int>
        get() = _verticalBlockCounts

    private val _indexInContainer = MutableLiveData<Int>()
    val indexInContainer: LiveData<Int>
        get() = _indexInContainer

    private val _unitsListShowed = MutableLiveData<Boolean>()
    val unitsListShowed: LiveData<Boolean>
        get() = _unitsListShowed

    private val _subSectionUnitBlocks = MutableStateFlow<List<Block>>(listOf())
    val subSectionUnitBlocks = _subSectionUnitBlocks.asStateFlow()

    var nextButtonText = ""
    var hasNextBlock = false

    private var currentMode: CourseViewMode? = null
    private var currentComponentId = ""
    private var courseName = ""

    private val _descendantsBlocks = MutableStateFlow<List<Block>>(listOf())
    val descendantsBlocks = _descendantsBlocks.asStateFlow()

    val hasNetworkConnection: Boolean
        get() = networkConnection.isOnline()

    fun loadBlocks(mode: CourseViewMode, componentId: String = "") {
        currentMode = mode
        viewModelScope.launch {
            try {
                val courseStructure = when (mode) {
                    CourseViewMode.FULL -> interactor.getCourseStructure(courseId)
                    CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos(courseId)
                }
                val blocks = courseStructure.blockData
                courseName = courseStructure.name
                this@CourseUnitContainerViewModel.blocks.clearAndAddAll(blocks)

                setupCurrentIndex(componentId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        _indexInContainer.value = 0

        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseStructureUpdated) {
                    if (event.courseId != courseId) return@collect

                    currentMode?.let { loadBlocks(it, currentComponentId) }
                    val blockId = blocks[currentVerticalIndex].id
                    _subSectionUnitBlocks.value =
                        getSubSectionUnitBlocks(blocks, getSubSectionId(blockId))
                }
            }
        }
    }

    private fun setupCurrentIndex(componentId: String = "") {
        if (currentSectionIndex != -1) return
        currentComponentId = componentId

        blocks.forEachIndexed { index, block ->
            if (block.id == unitId) {
                currentVerticalIndex = index
                currentSectionIndex = blocks.indexOfFirst {
                    it.descendants.contains(blocks[currentVerticalIndex].id)
                }
                if (block.descendants.isNotEmpty() || block.isGated()) {
                    _descendantsBlocks.value =
                        block.descendants.mapNotNull { descendant ->
                            blocks.firstOrNull { descendant == it.id }
                        }
                    _subSectionUnitBlocks.value =
                        getSubSectionUnitBlocks(blocks, getSubSectionId(unitId))

                    if (_descendantsBlocks.value.isEmpty()) {
                        _descendantsBlocks.value = listOf(block)
                    }
                } else {
                    setNextVerticalIndex()
                }
                if (currentVerticalIndex != -1) {
                    _verticalBlockCounts.value = blocks[currentVerticalIndex].descendants.size
                }
                if (componentId.isNotEmpty()) {
                    currentIndex = _descendantsBlocks.value.indexOfFirst { it.id == componentId }
                    _indexInContainer.value = currentIndex
                }
                return
            }
        }
    }

    private fun getSubSectionId(blockId: String): String {
        return blocks.firstOrNull { it.descendants.contains(blockId) }?.id ?: ""
    }

    private fun getSubSectionUnitBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = blocks.first { it.id == id }

        for (descendant in selectedBlock.descendants) {
            val blockDescendant = blocks.find {
                it.id == descendant
            }
            if (blockDescendant != null) {
                if (blockDescendant.type == BlockType.VERTICAL) {
                    resultList.add(blockDescendant.copy(type = getUnitType(blockDescendant.descendants)))
                }
            } else {
                continue
            }
        }
        return resultList
    }

    private fun getUnitType(descendant: List<String>): BlockType {
        val descendantBlocks = blocks.filter { descendant.contains(it.id) }

        return when {
            descendantBlocks.any { it.isProblemBlock } -> BlockType.PROBLEM
            descendantBlocks.any { it.isVideoBlock } -> BlockType.VIDEO
            descendantBlocks.any { it.isDiscussionBlock } -> BlockType.DISCUSSION
            else -> BlockType.OTHERS
        }
    }

    private fun setNextVerticalIndex() {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
    }

    fun proceedToNext() {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
        if (currentVerticalIndex != -1) {
            val sectionIndex = blocks.indexOfFirst {
                it.descendants.contains(blocks[currentVerticalIndex].id)
            }
            if (sectionIndex != currentSectionIndex) {
                currentSectionIndex = sectionIndex
                blocks.getOrNull(currentSectionIndex)?.id?.let {
                    sendCourseSectionChanged(it)
                }
            }
        }
    }

    fun getDownloadModelById(id: String): DownloadModel? = runBlocking(Dispatchers.IO) {
        return@runBlocking interactor.getDownloadModels().first()
            .find { it.id == id && it.downloadedState == DownloadedState.DOWNLOADED }
    }

    fun getCurrentBlock(): Block {
        return blocks[currentIndex]
    }

    fun moveToNextBlock(): Block? {
        return moveToBlock(currentIndex + 1)
    }

    fun moveToPrevBlock(): Block? {
        return moveToBlock(currentIndex - 1)
    }

    private fun moveToBlock(index: Int): Block? {
        _descendantsBlocks.value.getOrNull(index)?.let { block ->
            currentIndex = index
            if (currentVerticalIndex != -1) {
                _indexInContainer.value = currentIndex
            }
            return block
        }
        return null
    }

    private fun sendCourseSectionChanged(blockId: String) {
        viewModelScope.launch {
            notifier.send(CourseSectionChanged(blockId))
        }
    }

    fun getCurrentVerticalBlock(): Block? = blocks.getOrNull(currentVerticalIndex)

    fun getNextVerticalBlock(): Block? {
        val index = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
        return blocks.getOrNull(index)
    }

    fun getUnitBlocks(): List<Block> = _descendantsBlocks.value

    fun getSubSectionBlock(unitId: String): Block {
        return blocks.first { it.descendants.contains(unitId) }
    }

    fun courseUnitContainerShowedEvent() {
        analytics.logEvent(
            CourseAnalyticsEvent.UNIT_DETAIL.eventName,
            buildMap {
                put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.UNIT_DETAIL.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseName)
                put(CourseAnalyticsKey.BLOCK_ID.key, unitId)
            }
        )
    }

    fun nextBlockClickedEvent(blockId: String, blockName: String) {
        analytics.nextBlockClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun prevBlockClickedEvent(blockId: String, blockName: String) {
        analytics.prevBlockClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalClickedEvent(blockId: String, blockName: String) {
        analytics.finishVerticalClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalNextClickedEvent(blockId: String, blockName: String) {
        analytics.finishVerticalNextClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalBackClickedEvent() {
        analytics.finishVerticalBackClickedEvent(courseId, courseName)
    }

    fun setUnitsListVisibility(isVisible: Boolean) {
        _unitsListShowed.value = isVisible
    }
}
