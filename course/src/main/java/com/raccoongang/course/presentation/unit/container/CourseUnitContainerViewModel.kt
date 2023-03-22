package com.raccoongang.course.presentation.unit.container

import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.BlockType
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.module.db.DownloadModel
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.presentation.course.CourseViewMode
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class CourseUnitContainerViewModel(
    private val interactor: CourseInteractor,
    val courseId: String
) : BaseViewModel() {

    private val blocks = ArrayList<Block>()

    var currentIndex = 0
        private set
    var currentVerticalIndex = 0
        private set

    val isFirstIndexInContainer: Boolean
        get() {
            return blocks[currentVerticalIndex].descendants.first() == blocks[currentIndex].id
        }

    val isLastIndexInContainer: Boolean
        get() {
            return blocks[currentVerticalIndex].descendants.last() == blocks[currentIndex].id
        }

    var prevButtonText: String? = null
    var nextButtonText = ""
    var hasNextBlock = false

    fun loadBlocks(mode: CourseViewMode) {
        try {
            val blocks = when (mode) {
                CourseViewMode.FULL -> interactor.getCourseStructureFromCache()
                CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos()
            }
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
                return
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
        for (i in currentIndex + 1 until blocks.size) {
            val block = blocks[i]
            currentIndex = i
            updateVerticalIndex(block.id)
            return block
        }
        return null
    }

    fun moveToPrevBlock(): Block? {
        for (i in currentIndex - 1 downTo 0) {
            val block = blocks[i]
            currentIndex = i
            updateVerticalIndex(block.id)
            return block
        }
        return null
    }

    private fun updateVerticalIndex(blockId: String) {
        currentVerticalIndex =
            blocks.indexOfFirst { it.type == BlockType.VERTICAL && it.descendants.contains(blockId) }
    }
}