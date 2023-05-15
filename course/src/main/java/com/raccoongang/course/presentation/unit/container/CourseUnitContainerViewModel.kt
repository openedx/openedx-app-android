package com.raccoongang.course.presentation.unit.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.BlockType
import com.raccoongang.core.domain.model.Block
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
    var verticalIndex = 0
        private set

    val isFirstIndexInContainer: Boolean
        get() {
            return blocks.indexOfFirst { !it.type.isContainer() } == currentIndex
        }

    val isLastIndexInContainer: Boolean
        get() {
            return blocks[currentVerticalIndex].descendants.last() == blocks[currentIndex].id
        }


    private val _verticalBlockCounts = MutableLiveData<Int>()
    val verticalBlockCounts: LiveData<Int>
        get() = _verticalBlockCounts

    private val _indexInContainer = MutableLiveData<Int>()
    val indexInContainer: LiveData<Int>
        get() = _indexInContainer

    var nextButtonText = ""
    var hasNextBlock = false

    fun loadBlocks(mode: CourseViewMode) {
        try {
            val courseStructure = when (mode) {
                CourseViewMode.FULL -> interactor.getCourseStructureFromCache()
                CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos()
            }
            val blocks = courseStructure.blockData
            this.blocks.clear()
            this.blocks.addAll(blocks)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupCurrentIndex(blockId: String) {
        blocks.forEachIndexed { index, block ->
            if (block.id == blockId) {
                currentIndex = index
                updateVerticalIndex(blockId)
                if (currentVerticalIndex != -1) {
                    _indexInContainer.value =
                        blocks[currentVerticalIndex].descendants.indexOf(blocks[currentIndex].id)
                }
                return
            }
        }
    }

    fun proceedToNext(): Block? {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(verticalIndex) {
            it.type == BlockType.VERTICAL && it.id != blocks[verticalIndex].id
        }
        blocks[currentVerticalIndex].descendants.firstOrNull()?.let { id ->
            currentIndex = blocks.indexOfFirst { id == it.id } - 1
            return moveToNextBlock()
        }
        return null
    }

    fun getDownloadModelById(id: String): DownloadModel? = runBlocking(Dispatchers.IO) {
        return@runBlocking interactor.getDownloadModels().first()
            .find { it.id == id && it.downloadedState == DownloadedState.DOWNLOADED }
    }

    fun getCurrentBlock(): Block {
        return blocks[currentIndex]
    }

    fun moveToNextBlock(): Block? {
        for (i in currentIndex + 1 until blocks.size) {
            val block = blocks[i]
            currentIndex = i
            updateVerticalIndex(block.id)
            if (currentVerticalIndex != -1) {
                _indexInContainer.value =
                    blocks[currentVerticalIndex].descendants.indexOf(blocks[currentIndex].id)
            }
            return block
        }
        return null
    }

    fun moveToPrevBlock(): Block? {
        for (i in currentIndex - 1 downTo 0) {
            val block = blocks[i]
            if (!block.type.isContainer()) {
                currentIndex = i
                updateVerticalIndex(block.id)
                if (currentVerticalIndex != -1) {
                    _indexInContainer.value =
                        blocks[currentVerticalIndex].descendants.indexOf(blocks[currentIndex].id)
                }
                return block
            }
        }
        return null
    }

    fun getNextVerticalBlock(): Block? {
        val index = blocks.indexOfFirstFromIndex(verticalIndex) {
            it.type == BlockType.VERTICAL && it.id != blocks[verticalIndex].id
        }
        return blocks.getOrNull(index)
    }

    fun sendEventPauseVideo() {
        viewModelScope.launch {
            notifier.send(CoursePauseVideo())
        }
    }

    private fun updateVerticalIndex(blockId: String) {
        currentVerticalIndex =
            blocks.indexOfFirst { it.type == BlockType.VERTICAL && it.descendants.contains(blockId) }
        if (currentVerticalIndex != -1) {
            _verticalBlockCounts.value = blocks[currentVerticalIndex].descendants.size
            verticalIndex = currentVerticalIndex
        }
    }
}