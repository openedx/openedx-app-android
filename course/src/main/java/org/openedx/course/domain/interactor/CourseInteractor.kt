package org.openedx.course.domain.interactor

import kotlinx.coroutines.flow.Flow
import org.openedx.core.BlockType
import org.openedx.core.domain.interactor.CourseInteractor
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseStructure
import org.openedx.course.data.repository.CourseRepository

@Suppress("TooManyFunctions")
class CourseInteractor(
    private val repository: CourseRepository
) : CourseInteractor {

    suspend fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = true
    ): Flow<CourseStructure?> {
        return repository.getCourseStructureFlow(courseId, forceRefresh)
    }

    override suspend fun getCourseStructure(
        courseId: String,
        isNeedRefresh: Boolean
    ): CourseStructure {
        return repository.getCourseStructure(courseId, isNeedRefresh)
    }

    override suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        return repository.getCourseStructureFromCache(courseId)
    }

    suspend fun getEnrollmentDetailsFlow(courseId: String): Flow<CourseEnrollmentDetails?> {
        return repository.getEnrollmentDetailsFlow(courseId)
    }

    suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
        return repository.getEnrollmentDetails(courseId = courseId)
    }

    suspend fun getCourseStructureForVideos(
        courseId: String,
        isNeedRefresh: Boolean = false
    ): CourseStructure {
        val courseStructure = repository.getCourseStructure(courseId, isNeedRefresh)
        val blocks = courseStructure.blockData
        val videoBlocks = blocks.filter { it.type == BlockType.VIDEO }
        val resultBlocks = mutableListOf<Block>()

        videoBlocks.forEach { videoBlock ->
            val verticalBlock = findParentBlock(videoBlock.id, blocks) ?: return@forEach
            val sequentialBlock = findParentBlock(verticalBlock.id, blocks) ?: return@forEach
            val chapterBlock = findParentBlock(sequentialBlock.id, blocks) ?: return@forEach

            addToResultBlocks(videoBlock, verticalBlock, resultBlocks)
            addIfAbsent(resultBlocks, sequentialBlock)
            addIfAbsent(resultBlocks, chapterBlock)
        }

        return courseStructure.copy(blockData = resultBlocks)
    }

    private fun findParentBlock(childId: String, blocks: List<Block>): Block? {
        return blocks.firstOrNull { it.descendants.contains(childId) }
    }

    private fun addToResultBlocks(
        videoBlock: Block,
        verticalBlock: Block,
        resultBlocks: MutableList<Block>
    ) {
        resultBlocks.add(videoBlock)
        val verticalIndex = resultBlocks.indexOfFirst { it.id == verticalBlock.id }
        if (verticalIndex == -1) {
            resultBlocks.add(verticalBlock.copy(descendants = listOf(videoBlock.id)))
        } else {
            val block = resultBlocks[verticalIndex]
            resultBlocks[verticalIndex] =
                block.copy(descendants = block.descendants + videoBlock.id)
        }
    }

    private fun addIfAbsent(resultBlocks: MutableList<Block>, block: Block) {
        if (!resultBlocks.contains(block)) {
            resultBlocks.add(block)
        }
    }

    suspend fun getCourseStatusFlow(courseId: String) = repository.getCourseStatusFlow(courseId)

    suspend fun getCourseStatus(courseId: String) = repository.getCourseStatus(courseId)

    suspend fun getCourseDatesFlow(courseId: String) = repository.getCourseDatesFlow(courseId)

    suspend fun getCourseDates(courseId: String) = repository.getCourseDates(courseId)

    suspend fun resetCourseDates(courseId: String) = repository.resetCourseDates(courseId)

    suspend fun getDatesBannerInfo(courseId: String) = repository.getDatesBannerInfo(courseId)

    suspend fun getHandouts(courseId: String) = repository.getHandouts(courseId)

    suspend fun getAnnouncements(courseId: String) = repository.getAnnouncements(courseId)

    suspend fun removeDownloadModel(id: String) = repository.removeDownloadModel(id)

    fun getDownloadModels() = repository.getDownloadModels()

    override suspend fun getAllDownloadModels() = repository.getAllDownloadModels()

    suspend fun saveXBlockProgress(blockId: String, courseId: String, jsonProgress: String) {
        repository.saveOfflineXBlockProgress(blockId, courseId, jsonProgress)
    }

    suspend fun getXBlockProgress(blockId: String) = repository.getXBlockProgress(blockId)

    suspend fun submitAllOfflineXBlockProgress() = repository.submitAllOfflineXBlockProgress()

    suspend fun submitOfflineXBlockProgress(blockId: String, courseId: String) =
        repository.submitOfflineXBlockProgress(blockId, courseId)

    fun getCourseProgress(courseId: String, isRefresh: Boolean) =
        repository.getCourseProgress(courseId, isRefresh)
}
