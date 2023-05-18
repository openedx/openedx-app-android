package com.raccoongang.course.presentation.unit.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.BlockType
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.extension.clearAndAddAll
import com.raccoongang.core.extension.indexOfFirstFromIndex
import com.raccoongang.core.module.db.DownloadModel
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CoursePauseVideo
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CourseUnitContainerViewModel(
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    val courseId: String
) : BaseViewModel() {

    private val blocks = ArrayList<Block>()

    var currentIndex = 0
        private set
    var currentVerticalIndex = 0
        private set

    val isFirstIndexInContainer: Boolean
        get() {
            return descendants.first() == descendants[currentIndex]
        }

    val isLastIndexInContainer: Boolean
        get() {
            return descendants.last() == descendants[currentIndex]
        }

    private val _verticalBlockCounts = MutableLiveData<Int>()
    val verticalBlockCounts: LiveData<Int>
        get() = _verticalBlockCounts

    private val _indexInContainer = MutableLiveData<Int>()
    val indexInContainer: LiveData<Int>
        get() = _indexInContainer

    var nextButtonText = ""
    var hasNextBlock = false

    private val descendants = mutableListOf<String>()

    fun loadBlocks(mode: CourseViewMode) {
        try {
            val courseStructure = when (mode) {
                CourseViewMode.FULL -> interactor.getCourseStructureFromCache()
                CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos()
            }
            val blocks = courseStructure.blockData
            this.blocks.clearAndAddAll(blocks)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupCurrentIndex(blockId: String) {
        blocks.forEachIndexed { index, block ->
            if (block.id == blockId) {
                currentVerticalIndex = index
                if (block.descendants.isNotEmpty()) {
                    descendants.clearAndAddAll(block.descendants)
                } else {
                    setNextVerticalIndex()
                }
                if (currentVerticalIndex != -1) {
                    _indexInContainer.value = 0
                    _verticalBlockCounts.value = blocks[currentVerticalIndex].descendants.size
                }
                return
            }
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
    }

    fun getDownloadModelById(id: String): DownloadModel? = runBlocking(Dispatchers.IO) {
        return@runBlocking interactor.getDownloadModels().first()
            .find { it.id == id && it.downloadedState == DownloadedState.DOWNLOADED }
    }

    fun getCurrentBlock(): Block {
        return blocks[currentIndex]
    }

    fun moveToNextBlock(): Block? {
        for (i in currentIndex + 1 until descendants.size) {
            val block = blocks.firstOrNull { descendants[i] == it.id }
            currentIndex = i
            if (currentVerticalIndex != -1) {
                _indexInContainer.value = currentIndex
            }
            return block
        }
        return null
    }

    fun moveToPrevBlock(): Block? {
        for (i in currentIndex - 1 downTo 0) {
            val block = blocks.firstOrNull { descendants[i] == it.id }
            currentIndex = i
            if (currentVerticalIndex != -1) {
                _indexInContainer.value = currentIndex
            }
            return block
        }
        return null
    }

    fun sendEventPauseVideo() {
        viewModelScope.launch {
            notifier.send(CoursePauseVideo())
        }
    }

    fun getCurrentVerticalBlock(): Block? = blocks.getOrNull(currentVerticalIndex)

    fun getUnitBlocks(): List<Block> = blocks.filter { descendants.contains(it.id) }
}