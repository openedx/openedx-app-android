package org.openedx.course.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import org.openedx.core.ApiConstants
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.model.room.OfflineXBlockProgress
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.data.model.room.XBlockProgressData
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.CourseDao
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.system.connection.NetworkConnection
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for course data with request coalescing.
 *
 * When multiple callers request the same data simultaneously,
 * only one network request is made and all callers receive the same result.
 */
@Suppress("TooManyFunctions")
class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val networkConnection: NetworkConnection,
) {
    // Session tracking - when entering a course, mark that data needs refresh
    private val needsRefresh = ConcurrentHashMap.newKeySet<String>()

    private val structureCache = CoalescingCache<String, CourseStructure>(
        fetch = { courseId ->
            val response = api.getCourseStructure(
                "stale-if-error=0",
                "v4",
                preferencesManager.user?.username,
                courseId
            )
            courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
            response.mapToDomain()
        },
        persist = { courseId, _ -> needsRefresh.remove(courseId) }
    )

    private val statusCache = CoalescingCache<String, CourseComponentStatus>(
        fetch = { courseId ->
            val username = preferencesManager.user?.username ?: ""
            api.getCourseStatus(username, courseId).mapToDomain()
        }
    )

    private val datesCache = CoalescingCache<String, CourseDatesResult>(
        fetch = { courseId -> api.getCourseDates(courseId).getCourseDatesResult() }
    )

    private val progressCache = CoalescingCache<String, CourseProgress>(
        fetch = { courseId ->
            val response = api.getCourseProgress(courseId)
            courseDao.insertCourseProgressEntity(response.mapToRoomEntity(courseId))
            response.mapToDomain()
        }
    )

    private val enrollmentCache = CoalescingCache<String, CourseEnrollmentDetails>(
        fetch = { courseId -> api.getEnrollmentDetails(courseId).mapToDomain() },
        persist = { _, details -> courseDao.insertCourseEnrollmentDetailsEntity(details.mapToEntity()) }
    )

    /**
     * Call when entering a course to mark that data should be refreshed.
     */
    fun startCourseSession(courseId: String) {
        needsRefresh.add(courseId)
    }

    fun endCourseSession() {
        structureCache.clear()
        statusCache.clear()
        datesCache.clear()
        progressCache.clear()
        enrollmentCache.clear()
        needsRefresh.clear()
    }

    fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseStructure> = flow {
        // Always emit cached data first if available
        structureCache.getCached(courseId)?.let { emit(it) }

        if (structureCache.getCached(courseId) == null) {
            courseDao.getCourseStructureById(courseId)?.mapToDomain()?.let {
                structureCache.setCached(courseId, it)
                emit(it)
            }
        }

        val shouldRefresh = forceRefresh || needsRefresh.contains(courseId)
        if (networkConnection.isOnline() && (structureCache.getCached(courseId) == null || shouldRefresh)) {
            emit(structureCache.getOrFetch(courseId, forceRefresh = true))
        }

        if (structureCache.getCached(courseId) == null) {
            throw NoCachedDataException()
        }
    }

    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        return structureCache.getCached(courseId)
            ?: courseDao.getCourseStructureById(courseId)?.mapToDomain()?.also {
                structureCache.setCached(courseId, it)
            }
            ?: throw NoCachedDataException()
    }

    fun getEnrollmentDetailsFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseEnrollmentDetails> = flow {
        // Always emit cached data first if available
        enrollmentCache.getCached(courseId)?.let { emit(it) }

        if (enrollmentCache.getCached(courseId) == null) {
            courseDao.getCourseEnrollmentDetailsById(courseId)?.mapToDomain()?.let {
                enrollmentCache.setCached(courseId, it)
                emit(it)
            }
        }

        if (networkConnection.isOnline() && (enrollmentCache.getCached(courseId) == null || forceRefresh)) {
            emit(enrollmentCache.getOrFetch(courseId, forceRefresh = true))
        }

        if (enrollmentCache.getCached(courseId) == null) {
            throw NoCachedDataException()
        }
    }

    suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
        return api.getEnrollmentDetails(courseId).mapToDomain()
    }

    fun getCourseStatusFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseComponentStatus> = flow {
        // Always emit cached data first if available, otherwise emit empty status
        val cached = statusCache.getCached(courseId)
        emit(cached ?: CourseComponentStatus(""))

        val shouldRefresh = forceRefresh || needsRefresh.contains(courseId)
        if (networkConnection.isOnline() && (cached == null || shouldRefresh)) {
            emit(statusCache.getOrFetch(courseId, forceRefresh = true))
        }
    }

    suspend fun getCourseStatus(courseId: String): CourseComponentStatus {
        val username = preferencesManager.user?.username ?: ""
        val status = api.getCourseStatus(username, courseId).mapToDomain()
        statusCache.setCached(courseId, status)
        return status
    }

    fun getCourseDatesFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseDatesResult> = flow {
        // Always emit cached data first if available, otherwise emit empty result
        val cached = datesCache.getCached(courseId)
        emit(cached ?: emptyCourseDatesResult())

        val shouldRefresh = forceRefresh || needsRefresh.contains(courseId)
        if (networkConnection.isOnline() && (cached == null || shouldRefresh)) {
            emit(datesCache.getOrFetch(courseId, forceRefresh = true))
        }
    }

    suspend fun getCourseDates(courseId: String, forceRefresh: Boolean = false): CourseDatesResult {
        return when {
            !forceRefresh && datesCache.getCached(courseId) != null -> datesCache.getCached(courseId)!!
            networkConnection.isOnline() -> datesCache.getOrFetch(courseId, forceRefresh = true)
            else -> datesCache.getCached(courseId) ?: throw NoCachedDataException()
        }
    }

    private fun emptyCourseDatesResult() = CourseDatesResult(
        datesSection = linkedMapOf(),
        courseBanner = CourseDatesBannerInfo(
            missedDeadlines = false,
            missedGatedContent = false,
            verifiedUpgradeLink = "",
            contentTypeGatingEnabled = false,
            hasEnded = false
        )
    )

    fun getCourseProgress(
        courseId: String,
        isRefresh: Boolean,
        getOnlyCacheIfExist: Boolean
    ): Flow<CourseProgress> = flow {
        if (!isRefresh) {
            progressCache.getCached(courseId)?.let { emit(it) }
        }

        if (!isRefresh && progressCache.getCached(courseId) == null) {
            courseDao.getCourseProgressById(courseId)?.mapToDomain()?.let {
                progressCache.setCached(courseId, it)
                emit(it)
            }
        }

        val shouldRefresh = isRefresh || needsRefresh.contains(courseId)
        val hasCache = progressCache.getCached(courseId) != null
        val shouldFetch = shouldRefresh || !hasCache || !getOnlyCacheIfExist

        if (networkConnection.isOnline() && shouldFetch) {
            emit(progressCache.getOrFetch(courseId, forceRefresh = true))
        }
    }

    suspend fun markBlocksCompletion(courseId: String, blocksId: List<String>) {
        val username = preferencesManager.user?.username ?: ""
        val blocksCompletionBody = BlocksCompletionBody(
            username,
            courseId,
            blocksId.associateWith { "1" }
        )
        api.markBlocksCompletion(blocksCompletionBody)
    }

    suspend fun resetCourseDates(courseId: String) =
        api.resetCourseDates(mapOf(ApiConstants.COURSE_KEY to courseId)).mapToDomain()

    suspend fun getDatesBannerInfo(courseId: String) =
        api.getDatesBannerInfo(courseId).mapToDomain()

    suspend fun getHandouts(courseId: String) = api.getHandouts(courseId).mapToDomain()

    suspend fun getAnnouncements(courseId: String) =
        api.getAnnouncements(courseId).map { it.mapToDomain() }

    suspend fun removeDownloadModel(id: String) {
        downloadDao.removeDownloadModel(id)
    }

    fun getDownloadModels() = downloadDao.getAllDataFlow().map { list ->
        list.map { it.mapToDomain() }
    }

    suspend fun getAllDownloadModels() = downloadDao.readAllData().map { it.mapToDomain() }

    suspend fun saveOfflineXBlockProgress(blockId: String, courseId: String, jsonProgress: String) {
        val offlineXBlockProgress = OfflineXBlockProgress(
            blockId = blockId,
            courseId = courseId,
            jsonProgress = XBlockProgressData.parseJson(jsonProgress)
        )
        downloadDao.insertOfflineXBlockProgress(offlineXBlockProgress)
    }

    suspend fun getXBlockProgress(blockId: String) = downloadDao.getOfflineXBlockProgress(blockId)

    suspend fun submitAllOfflineXBlockProgress() {
        val allOfflineXBlockProgress = downloadDao.getAllOfflineXBlockProgress()
        allOfflineXBlockProgress.forEach {
            submitOfflineXBlockProgress(it.blockId, it.courseId, it.jsonProgress.data)
        }
    }

    suspend fun submitOfflineXBlockProgress(blockId: String, courseId: String) {
        val jsonProgressData = getXBlockProgress(blockId)?.jsonProgress?.data
        submitOfflineXBlockProgress(blockId, courseId, jsonProgressData)
    }

    private suspend fun submitOfflineXBlockProgress(
        blockId: String,
        courseId: String,
        jsonProgressData: String?
    ) {
        if (!jsonProgressData.isNullOrEmpty()) {
            val parts = mutableListOf<MultipartBody.Part>()
            val decodedQuery = URLDecoder.decode(jsonProgressData, StandardCharsets.UTF_8.name())
            val keyValuePairs = decodedQuery.split("&")
            for (pair in keyValuePairs) {
                val (key, value) = pair.split("=")
                parts.add(MultipartBody.Part.createFormData(key, value))
            }
            api.submitOfflineXBlockProgress(courseId, blockId, parts)
            downloadDao.removeOfflineXBlockProgress(listOf(blockId))
        }
    }

    suspend fun saveVideoProgress(
        blockId: String,
        videoUrl: String,
        videoTime: Long,
        duration: Long
    ) {
        val videoProgressEntity = VideoProgressEntity(blockId, videoUrl, videoTime, duration)
        courseDao.insertVideoProgressEntity(videoProgressEntity)
    }

    suspend fun getVideoProgress(blockId: String): VideoProgressEntity {
        return courseDao.getVideoProgressByBlockId(blockId)
            ?: VideoProgressEntity(blockId, "", null, null)
    }
}
