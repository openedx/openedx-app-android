package org.openedx.course.data.repository

import kotlinx.coroutines.CompletableDeferred
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
 * This is achieved using CompletableDeferred.
 */
@Suppress("TooManyFunctions")
class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val networkConnection: NetworkConnection,
) {
    private val structureCache = ConcurrentHashMap<String, CourseStructure>()
    private val statusCache = ConcurrentHashMap<String, CourseComponentStatus>()
    private val datesCache = ConcurrentHashMap<String, CourseDatesResult>()
    private val progressCache = ConcurrentHashMap<String, CourseProgress>()
    private val enrollmentCache = ConcurrentHashMap<String, CourseEnrollmentDetails>()

    // Pending requests - when a request is in progress, other callers wait for it
    private val pendingStructure = ConcurrentHashMap<String, CompletableDeferred<CourseStructure>>()
    private val pendingStatus =
        ConcurrentHashMap<String, CompletableDeferred<CourseComponentStatus>>()
    private val pendingDates = ConcurrentHashMap<String, CompletableDeferred<CourseDatesResult>>()
    private val pendingProgress = ConcurrentHashMap<String, CompletableDeferred<CourseProgress>>()
    private val pendingEnrollment =
        ConcurrentHashMap<String, CompletableDeferred<CourseEnrollmentDetails>>()

    // Session tracking - when entering a course, mark that data needs refresh
    private val needsRefresh = ConcurrentHashMap.newKeySet<String>()

    private fun <T> getOrCreateDeferred(
        cache: ConcurrentHashMap<String, CompletableDeferred<T>>,
        key: String
    ): Pair<CompletableDeferred<T>, Boolean> {
        cache[key]?.let { return it to false }
        val deferred = CompletableDeferred<T>()
        val existing = cache.putIfAbsent(key, deferred)
        return if (existing != null) existing to false else deferred to true
    }

    /**
     * Call when entering a course to mark that data should be refreshed.
     */
    fun startCourseSession(courseId: String) {
        needsRefresh.add(courseId)
    }

    fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseStructure> = flow {
        if (!forceRefresh) {
            structureCache[courseId]?.let { emit(it) }

            if (structureCache[courseId] == null) {
                courseDao.getCourseStructureById(courseId)?.mapToDomain()?.let {
                    structureCache[courseId] = it
                    emit(it)
                }
            }
        }

        val shouldRefresh = forceRefresh || needsRefresh.contains(courseId)
        if (networkConnection.isOnline() && (structureCache[courseId] == null || shouldRefresh)) {
            emit(fetchOrAwaitStructure(courseId))
        }

        if (structureCache[courseId] == null) {
            throw NoCachedDataException()
        }
    }

    private suspend fun fetchOrAwaitStructure(courseId: String): CourseStructure {
        val (deferred, isOwner) = getOrCreateDeferred(pendingStructure, courseId)
        return if (isOwner) {
            try {
                val response = api.getCourseStructure(
                    "stale-if-error=0",
                    "v4",
                    preferencesManager.user?.username,
                    courseId
                )
                courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
                val structure = response.mapToDomain()
                structureCache[courseId] = structure
                needsRefresh.remove(courseId)
                deferred.complete(structure)
                structure
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pendingStructure.remove(courseId)
            }
        } else {
            deferred.await()
        }
    }

    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        return structureCache[courseId]
            ?: courseDao.getCourseStructureById(courseId)?.mapToDomain()?.also {
                structureCache[courseId] = it
            }
            ?: throw NoCachedDataException()
    }

    fun getEnrollmentDetailsFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseEnrollmentDetails> = flow {
        if (!forceRefresh) {
            enrollmentCache[courseId]?.let { emit(it) }

            if (enrollmentCache[courseId] == null) {
                courseDao.getCourseEnrollmentDetailsById(courseId)?.mapToDomain()?.let {
                    enrollmentCache[courseId] = it
                    emit(it)
                }
            }
        }

        if (networkConnection.isOnline() && (enrollmentCache[courseId] == null || forceRefresh)) {
            emit(fetchOrAwaitEnrollment(courseId))
        }

        if (enrollmentCache[courseId] == null) {
            throw NoCachedDataException()
        }
    }

    private suspend fun fetchOrAwaitEnrollment(courseId: String): CourseEnrollmentDetails {
        val (deferred, isOwner) = getOrCreateDeferred(pendingEnrollment, courseId)
        return if (isOwner) {
            try {
                val details = api.getEnrollmentDetails(courseId).mapToDomain()
                courseDao.insertCourseEnrollmentDetailsEntity(details.mapToEntity())
                enrollmentCache[courseId] = details
                deferred.complete(details)
                details
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pendingEnrollment.remove(courseId)
            }
        } else {
            deferred.await()
        }
    }

    suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
        return api.getEnrollmentDetails(courseId).mapToDomain()
    }

    fun getCourseStatusFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseComponentStatus> = flow {
        if (!forceRefresh) {
            statusCache[courseId]?.let { emit(it) }
        }

        val shouldRefresh = forceRefresh || needsRefresh.contains(courseId)
        if (networkConnection.isOnline() && (statusCache[courseId] == null || shouldRefresh)) {
            emit(fetchOrAwaitStatus(courseId))
        } else if (statusCache[courseId] == null) {
            emit(CourseComponentStatus(""))
        }
    }

    private suspend fun fetchOrAwaitStatus(courseId: String): CourseComponentStatus {
        val (deferred, isOwner) = getOrCreateDeferred(pendingStatus, courseId)
        return if (isOwner) {
            try {
                val username = preferencesManager.user?.username ?: ""
                val status = api.getCourseStatus(username, courseId).mapToDomain()
                statusCache[courseId] = status
                deferred.complete(status)
                status
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pendingStatus.remove(courseId)
            }
        } else {
            deferred.await()
        }
    }

    suspend fun getCourseStatus(courseId: String): CourseComponentStatus {
        val username = preferencesManager.user?.username ?: ""
        val status = api.getCourseStatus(username, courseId).mapToDomain()
        statusCache[courseId] = status
        return status
    }

    fun getCourseDatesFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseDatesResult> = flow {
        if (!forceRefresh) {
            datesCache[courseId]?.let { emit(it) }
        }

        val shouldRefresh = forceRefresh || needsRefresh.contains(courseId)
        if (networkConnection.isOnline() && (datesCache[courseId] == null || shouldRefresh)) {
            emit(fetchOrAwaitDates(courseId))
        } else if (datesCache[courseId] == null) {
            emit(emptyCourseDatesResult())
        }
    }

    private suspend fun fetchOrAwaitDates(courseId: String): CourseDatesResult {
        val (deferred, isOwner) = getOrCreateDeferred(pendingDates, courseId)
        return if (isOwner) {
            try {
                val datesResult = api.getCourseDates(courseId).getCourseDatesResult()
                datesCache[courseId] = datesResult
                deferred.complete(datesResult)
                datesResult
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pendingDates.remove(courseId)
            }
        } else {
            deferred.await()
        }
    }

    suspend fun getCourseDates(courseId: String, forceRefresh: Boolean = false): CourseDatesResult {
        return when {
            !forceRefresh && datesCache[courseId] != null -> datesCache[courseId]!!
            networkConnection.isOnline() -> fetchOrAwaitDates(courseId)
            else -> datesCache[courseId] ?: throw NoCachedDataException()
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
            progressCache[courseId]?.let { emit(it) }
        }

        if (!isRefresh && progressCache[courseId] == null) {
            courseDao.getCourseProgressById(courseId)?.mapToDomain()?.let {
                progressCache[courseId] = it
                emit(it)
            }
        }

        val shouldRefresh = isRefresh || needsRefresh.contains(courseId)
        val hasCache = progressCache[courseId] != null
        val shouldFetch = shouldRefresh || !hasCache || !getOnlyCacheIfExist

        if (networkConnection.isOnline() && shouldFetch) {
            emit(fetchOrAwaitProgress(courseId))
        }
    }

    private suspend fun fetchOrAwaitProgress(courseId: String): CourseProgress {
        val (deferred, isOwner) = getOrCreateDeferred(pendingProgress, courseId)
        return if (isOwner) {
            try {
                val response = api.getCourseProgress(courseId)
                courseDao.insertCourseProgressEntity(response.mapToRoomEntity(courseId))
                val progress = response.mapToDomain()
                progressCache[courseId] = progress
                deferred.complete(progress)
                progress
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pendingProgress.remove(courseId)
            }
        } else {
            deferred.await()
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
