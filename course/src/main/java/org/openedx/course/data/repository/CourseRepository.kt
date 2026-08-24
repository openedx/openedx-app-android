package org.openedx.course.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MultipartBody
import org.openedx.core.ApiConstants
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.model.room.CourseStructureEntity
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
import java.util.concurrent.atomic.AtomicLong

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
    /**
     * Identifies state owned by one course session generation. Old completions must address their
     * original key so they cannot alter a new session's refresh marker or completion version.
     */
    private data class CourseSessionKey(
        val sessionGeneration: Long,
        val courseId: String,
    )

    /**
     * Carries a cache-first response through persistence. When a fresh result wins while this
     * response is in flight, [returnedCourseStructure] is replaced before the coalesced request
     * completes so Flow callers cannot emit the stale fetched value after the fresh commit.
     */
    private data class NonFreshFetchResult(
        val capturedSessionGeneration: Long,
        val capturedFreshCompletionVersion: Long,
        val roomEntity: CourseStructureEntity?,
        val fetchedCourseStructure: CourseStructure,
    ) {
        var returnedCourseStructure: CourseStructure = fetchedCourseStructure
    }

    /**
     * Pairs the domain object and Room entity from one fresh response. The pair stays together
     * until it is saved, so a later fetch cannot combine the earlier domain object with a newer
     * Room entity.
     */
    private data class FreshFetchResult(
        val capturedSessionGeneration: Long,
        val courseStructure: CourseStructure,
        val roomEntity: CourseStructureEntity,
    )

    /**
     * Cache entries are valid only for the current session generation. A mutex per course
     * serializes non-fresh and fresh writes, plus publishing values read from Room. Keep each
     * mutex after reset because an old DAO insert may already be running. A new-session write
     * must follow that insert so its value is final.
     */
    private val sessionGeneration = AtomicLong(0)
    private val courseWriteMutexes = ConcurrentHashMap<String, Mutex>()
    private val freshCompletionVersion = ConcurrentHashMap<CourseSessionKey, AtomicLong>()

    /**
     * Keys include the session generation so an old completion cannot remove a new session's marker.
     */
    private val needsRefresh = ConcurrentHashMap.newKeySet<CourseSessionKey>()

    private val structureCache: CoalescingCache<CourseSessionKey, NonFreshFetchResult> = CoalescingCache(
        fetch = { courseSessionKey ->
            val capturedGeneration = courseSessionKey.sessionGeneration
            val courseId = courseSessionKey.courseId
            val capturedFreshCompletionVersion =
                freshCompletionVersionFor(capturedGeneration, courseId).get()
            val response = api.getCourseStructure(
                "stale-if-error=0",
                "v4",
                preferencesManager.user?.username,
                courseId
            )
            NonFreshFetchResult(
                capturedSessionGeneration = capturedGeneration,
                capturedFreshCompletionVersion = capturedFreshCompletionVersion,
                roomEntity = response.mapToRoomEntity(),
                fetchedCourseStructure = response.mapToDomain(),
            )
        },
        persist = { courseSessionKey, fetchResult ->
            val courseId = courseSessionKey.courseId

            runUnderCourseWriteGuard(courseId, fetchResult.capturedSessionGeneration) {
                val currentFreshCompletionVersion = freshCompletionVersionFor(
                    fetchResult.capturedSessionGeneration,
                    courseId,
                ).get()
                if (currentFreshCompletionVersion !=
                    fetchResult.capturedFreshCompletionVersion
                ) {
                    getCachedStructure(courseId)?.let {
                        fetchResult.returnedCourseStructure = it
                    }
                    return@runUnderCourseWriteGuard
                }

                fetchResult.roomEntity?.let { courseDao.insertCourseStructureEntity(it) }
                setCachedStructure(
                    courseId,
                    fetchResult.fetchedCourseStructure,
                    fetchResult.capturedSessionGeneration,
                )
            }
            needsRefresh.remove(courseSessionKey)
        },
        // Cache writes are explicit so a stale fetch cannot replace a fresh or new-session value.
        autoCache = false,
        activeGeneration = { sessionGeneration.get() },
    )

    /**
     * Fresh requests use a separate coalescing instance so they always send `no-cache` and never
     * join a non-fresh request that uses `stale-if-error=0`.
     */
    private val freshStructureCache = CoalescingCache<CourseSessionKey, FreshFetchResult>(
        fetch = { courseSessionKey ->
            val courseId = courseSessionKey.courseId
            val response = api.getCourseStructure(
                "no-cache",
                "v4",
                preferencesManager.user?.username,
                courseId,
            )
            FreshFetchResult(
                capturedSessionGeneration = courseSessionKey.sessionGeneration,
                courseStructure = response.mapToDomain(),
                roomEntity = response.mapToRoomEntity(),
            )
        },
        persist = { courseSessionKey, freshResult ->
            val courseId = courseSessionKey.courseId
            runUnderCourseWriteGuard(courseId, courseSessionKey.sessionGeneration) {
                courseDao.insertCourseStructureEntity(freshResult.roomEntity)
                freshCompletionVersionFor(
                    courseSessionKey.sessionGeneration,
                    courseId,
                ).incrementAndGet()
                setCachedStructure(
                    courseId,
                    freshResult.courseStructure,
                    courseSessionKey.sessionGeneration,
                )
            }
            needsRefresh.remove(courseSessionKey)
        },
        autoCache = false,
        activeGeneration = { sessionGeneration.get() },
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
        val courseSessionKey = CourseSessionKey(sessionGeneration.get(), courseId)
        needsRefresh.add(courseSessionKey)
    }

    fun endCourseSession() {
        sessionGeneration.incrementAndGet()
        structureCache.cancelPending()
        freshStructureCache.cancelPending()
        structureCache.clear()
        freshStructureCache.clear()
        statusCache.clear()
        datesCache.clear()
        progressCache.clear()
        enrollmentCache.clear()
        needsRefresh.clear()
        freshCompletionVersion.clear()
    }

    fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = false
    ): Flow<CourseStructure> = flow {
        val flowSessionGeneration = sessionGeneration.get()

        // Always emit cached data first if available
        getCachedStructure(courseId)?.let { emit(it) }

        if (getCachedStructure(courseId) == null) {
            val capturedFreshCompletionVersion =
                freshCompletionVersionFor(flowSessionGeneration, courseId).get()
            val roomStructure = courseDao.getCourseStructureById(courseId)?.mapToDomain()
            if (roomStructure != null) {
                val structureToEmit = runUnderCourseWriteGuard(
                    courseId,
                    flowSessionGeneration,
                ) {
                    resolveStructureFromRoom(
                        courseId = courseId,
                        capturedGeneration = flowSessionGeneration,
                        capturedFreshCompletionVersion = capturedFreshCompletionVersion,
                        roomStructure = roomStructure,
                    )
                }
                structureToEmit?.let { emit(it) }
            }
        }

        val courseSessionKey = CourseSessionKey(flowSessionGeneration, courseId)
        val shouldRefresh = forceRefresh || needsRefresh.contains(courseSessionKey)
        val hasCachedStructure = getCachedStructure(courseId) != null
        val shouldFetch = networkConnection.isOnline() && (!hasCachedStructure || shouldRefresh)
        if (shouldFetch) {
            val fetchResult = structureCache.getOrFetch(courseSessionKey, forceRefresh = true)
            emit(fetchResult.returnedCourseStructure)
        }

        if (getCachedStructure(courseId) == null) {
            throw NoCachedDataException()
        }
    }

    suspend fun getCourseStructureFresh(courseId: String): CourseStructure {
        val courseSessionKey = CourseSessionKey(sessionGeneration.get(), courseId)
        return freshStructureCache.getOrFetch(courseSessionKey, forceRefresh = true).courseStructure
    }

    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        val cachedStructure = getCachedStructure(courseId)
        if (cachedStructure != null) {
            return cachedStructure
        }

        val capturedGeneration = sessionGeneration.get()
        val capturedFreshCompletionVersion =
            freshCompletionVersionFor(capturedGeneration, courseId).get()
        val roomEntity = courseDao.getCourseStructureById(courseId)
            ?: throw NoCachedDataException()
        val roomStructure = roomEntity.mapToDomain()

        val resolvedStructure = runUnderCourseWriteGuard(courseId, capturedGeneration) {
            resolveStructureFromRoom(
                courseId = courseId,
                capturedGeneration = capturedGeneration,
                capturedFreshCompletionVersion = capturedFreshCompletionVersion,
                roomStructure = roomStructure,
            )
        }
        if (resolvedStructure == null) {
            throw NoCachedDataException()
        }
        return resolvedStructure
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

        val capturedGeneration = sessionGeneration.get()
        val courseSessionKey = CourseSessionKey(capturedGeneration, courseId)
        val shouldRefresh = forceRefresh || needsRefresh.contains(courseSessionKey)
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

        val capturedGeneration = sessionGeneration.get()
        val courseSessionKey = CourseSessionKey(capturedGeneration, courseId)
        val shouldRefresh = forceRefresh || needsRefresh.contains(courseSessionKey)
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

        val capturedGeneration = sessionGeneration.get()
        val courseSessionKey = CourseSessionKey(capturedGeneration, courseId)
        val shouldRefresh = isRefresh || needsRefresh.contains(courseSessionKey)
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

    private fun freshCompletionVersionFor(
        generation: Long,
        courseId: String,
    ): AtomicLong {
        val courseSessionKey = CourseSessionKey(generation, courseId)
        return freshCompletionVersion.getOrPut(courseSessionKey) { AtomicLong(0) }
    }

    private fun getCachedStructure(
        courseId: String,
    ): CourseStructure? {
        return structureCache.getCached(
            CourseSessionKey(sessionGeneration.get(), courseId),
        )?.returnedCourseStructure
    }

    private fun setCachedStructure(
        courseId: String,
        courseStructure: CourseStructure,
        generation: Long,
    ) {
        structureCache.setCached(
            CourseSessionKey(generation, courseId),
            NonFreshFetchResult(
                capturedSessionGeneration = generation,
                capturedFreshCompletionVersion = freshCompletionVersionFor(generation, courseId).get(),
                roomEntity = null,
                fetchedCourseStructure = courseStructure,
            ),
            generation,
        )
    }

    private suspend fun <R> runUnderCourseWriteGuard(
        courseId: String,
        capturedGeneration: Long,
        block: suspend () -> R,
    ): R? {
        val courseWriteMutex = courseWriteMutexes.getOrPut(courseId) { Mutex() }
        return courseWriteMutex.withLock {
            if (sessionGeneration.get() != capturedGeneration) {
                return@withLock null
            }
            block()
        }
    }

    private fun resolveStructureFromRoom(
        courseId: String,
        capturedGeneration: Long,
        capturedFreshCompletionVersion: Long,
        roomStructure: CourseStructure,
    ): CourseStructure? {
        val currentCachedStructure = getCachedStructure(courseId)
        val currentFreshCompletionVersion =
            freshCompletionVersionFor(capturedGeneration, courseId).get()

        return when {
            currentFreshCompletionVersion != capturedFreshCompletionVersion -> {
                currentCachedStructure
            }

            currentCachedStructure != null -> {
                currentCachedStructure
            }

            else -> {
                setCachedStructure(courseId, roomStructure, capturedGeneration)
                roomStructure
            }
        }
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
