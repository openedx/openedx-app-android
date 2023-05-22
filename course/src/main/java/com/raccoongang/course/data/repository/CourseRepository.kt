package com.raccoongang.course.data.repository

import com.raccoongang.core.data.api.CourseApi
import com.raccoongang.core.data.model.BlocksCompletionBody
import com.raccoongang.core.data.model.EnrollBody
import com.raccoongang.core.data.model.room.CourseEntity
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.*
import com.raccoongang.core.exception.NoCachedDataException
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.course.data.storage.CourseDao
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody

class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: PreferencesManager,
) {
    private var courseStructure: CourseStructure? = null

    suspend fun getCourseDetail(id: String): Course {
        val course = api.getCourseDetail(id)
        courseDao.updateCourseEntity(CourseEntity.createFrom(course))
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
        courseDao.updateCourseStructureEntity(response.mapToRoomEntity())
        courseStructure = null
        courseStructure = response.mapToDomain()
    }

    suspend fun preloadCourseStructureFromCache(courseId: String) {
        val cachedCourseStructure = courseDao.getCourseStructureById(courseId)
        courseStructure = null
        if (cachedCourseStructure != null) {
            courseStructure = cachedCourseStructure.mapToDomain()
        } else {
            throw NoCachedDataException()
        }
    }

    @Throws(IllegalStateException::class)
    fun getCourseStructureFromCache(): CourseStructure {
        if (courseStructure != null) {
            return courseStructure!!
        } else {
            throw IllegalStateException("Course structure is empty")
        }
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

    suspend fun getHandouts(courseId: String) = api.getHandouts(courseId).mapToDomain()

    suspend fun getAnnouncements(courseId: String) =
        api.getAnnouncements(courseId).map { it.mapToDomain() }

}