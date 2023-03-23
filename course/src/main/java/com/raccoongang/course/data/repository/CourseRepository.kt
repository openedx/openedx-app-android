package com.raccoongang.course.data.repository

import com.raccoongang.core.data.api.CourseApi
import com.raccoongang.core.data.model.BlocksCompletionBody
import com.raccoongang.core.data.model.EnrollBody
import com.raccoongang.core.data.model.room.CourseEntity
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.*
import com.raccoongang.core.exception.NoCachedDataException
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.course.data.model.BlockDbEntity
import com.raccoongang.course.data.storage.CourseDao
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import java.text.DecimalFormat

class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: PreferencesManager,
) {
    private val blocksList = mutableListOf<Block>()

    suspend fun getCourseDetail(id: String): Course {
        val course = api.getCourseDetail(id)
        courseDao.insertCourseEntity(CourseEntity.createFrom(course))
        return course.mapToDomain()
    }

    suspend fun getCourseDetailFromCache(id: String): Course? {
        return courseDao.getCourseById(id)?.mapToDomain()
    }

    suspend fun removeDownloadModel(id: String) {
        downloadDao.removeDownloadModel(id)
    }

    fun getDownloadModels() = downloadDao.readAllData().map { list ->
        list.map { it.mapToDomain() }
    }

    suspend fun enrollInACourse(courseId: String): ResponseBody {
        val enrollBody = EnrollBody(
            EnrollBody.CourseDetails(
                courseId = courseId,
                emailOptIn = preferencesManager.user?.email
            )
        )
        return api.enrollInACourse(enrollBody)
    }

    suspend fun preloadCourseStructure(courseId: String) {
        val response = api.getCourseStructure(
            "stale-if-error=0",
            "v1",
            preferencesManager.user?.username,
            courseId
        )
        val courseStructure = response.mapToDomain()
        courseDao.insertCourseBlocks(
            *response.blockData.values
                .map {
                    BlockDbEntity.createFrom(it, courseId)
                }.toTypedArray()
        )
        blocksList.clear()
        blocksList.addAll(courseStructure.blockData.values.toList())
    }

    suspend fun preloadCourseStructureFromCache(courseId: String) {
        val response = courseDao.getCourseBlocksById(courseId)
        blocksList.clear()
        if (!response.isNullOrEmpty()) {
            blocksList.addAll(response.map { it.mapToDomain() })
        } else {
            throw NoCachedDataException()
        }
    }

    @Throws(IllegalStateException::class)
    fun getCourseStructureFromCache(): List<Block> {
        if (blocksList.isNotEmpty()) {
            return blocksList
        } else {
            throw IllegalStateException("Course structure is empty")
        }
    }

    suspend fun getEnrolledCourseById(courseId: String): EnrolledCourse? {
        val user = preferencesManager.user
        val enrolledCourse = api.getEnrolledCourses(
            username = user?.username ?: ""
        )
        val course = enrolledCourse.find {
            it.course?.id == courseId
        }
        return course?.mapToDomain()
    }

    suspend fun getEnrolledCourseFromCacheById(courseId: String): EnrolledCourse? {
        val course = courseDao.getEnrolledCourseById(courseId)
        return course?.mapToDomain()
    }

    suspend fun getCourseStatus(courseId: String): CourseComponentStatus {
        val username = preferencesManager.user?.username ?: ""
        return api.getCourseStatus(username, courseId).mapToDomain()
    }

    suspend fun markBlocksCompletion(courseId: String, blocksId: List<String>) {
        val username = preferencesManager.user?.username ?: ""
        val blocksCompletionBody = BlocksCompletionBody(
            username,
            courseId,
            blocksId.associateWith { "1" }.toMap()
        )
        return api.markBlocksCompletion(blocksCompletionBody)
    }

    suspend fun getHandouts(url: String) = api.getHandouts(url).mapToDomain()

    suspend fun getAnnouncements(url: String) = api.getAnnouncements(url).map { it.mapToDomain() }

    suspend fun getProgress(courseId: String) = api.getProgress(courseId).mapToDomain(DecimalFormat("0.#"))
}