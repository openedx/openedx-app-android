package com.raccoongang.course.domain.interactor

import com.raccoongang.core.BlockType
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.module.db.DownloadModel
import com.raccoongang.course.data.repository.CourseRepository

class CourseInteractor(
    private val repository: CourseRepository
) {

    suspend fun getCourseDetails(id: String) = repository.getCourseDetail(id)

    suspend fun getCourseDetailsFromCache(id: String) = repository.getCourseDetailFromCache(id)

    suspend fun enrollInACourse(id: String) {
        repository.enrollInACourse(courseId = id)
    }

    suspend fun preloadCourseStructure(courseId: String) =
        repository.preloadCourseStructure(courseId)

    suspend fun preloadCourseStructureFromCache(courseId: String) =
        repository.preloadCourseStructureFromCache(courseId)

    @Throws(IllegalStateException::class)
    fun getCourseStructureFromCache() = repository.getCourseStructureFromCache()

    @Throws(IllegalStateException::class)
    fun getCourseStructureForVideos(): List<Block> {
        val blocks = repository.getCourseStructureFromCache()
        val videoBlocks = blocks.filter { it.type == BlockType.VIDEO }
        val resultBlocks = ArrayList<Block>()
        videoBlocks.forEach { videoBlock ->
            val verticalBlock = blocks.firstOrNull { it.descendants.contains(videoBlock.id) }
            if (verticalBlock != null) {
                val sequentialBlock =
                    blocks.firstOrNull { it.descendants.contains(verticalBlock.id) }
                if (sequentialBlock != null) {
                    val chapterBlock =
                        blocks.firstOrNull { it.descendants.contains(sequentialBlock.id) }
                    if (chapterBlock != null) {
                        resultBlocks.add(videoBlock)
                        if (!resultBlocks.contains(verticalBlock)) {
                            resultBlocks.add(verticalBlock.copy(descendants = listOf(videoBlock.id)))
                        } else {
                            val index = resultBlocks.indexOfFirst { it.id == verticalBlock.id }
                            if (index != -1) {
                                val block = resultBlocks[index]
                                resultBlocks[index] =
                                    block.copy(descendants = block.descendants + videoBlock.id)
                            }
                        }
                        if (!resultBlocks.contains(sequentialBlock)) {
                            resultBlocks.add(sequentialBlock)
                        }
                        if (!resultBlocks.contains(chapterBlock)) {
                            resultBlocks.add(chapterBlock)
                        }
                    }
                }

            }
        }
        return resultBlocks.toList()
    }

    suspend fun getEnrolledCourseById(courseId: String) = repository.getEnrolledCourseById(courseId)

    suspend fun getEnrolledCourseFromCacheById(courseId: String) =
        repository.getEnrolledCourseFromCacheById(courseId)

    suspend fun getCourseStatus(courseId: String) = repository.getCourseStatus(courseId)

    suspend fun getHandouts(url: String) = repository.getHandouts(url)

    suspend fun getAnnouncements(url: String) = repository.getAnnouncements(url)

    suspend fun removeDownloadModel(id: String) = repository.removeDownloadModel(id)

    fun getDownloadModels() = repository.getDownloadModels()

}
