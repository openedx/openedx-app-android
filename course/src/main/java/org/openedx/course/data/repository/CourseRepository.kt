package org.openedx.course.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import org.openedx.core.ApiConstants
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.model.room.OfflineXBlockProgress
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
import org.openedx.core.extension.channelFlowWithAwait
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.system.connection.NetworkConnection
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Suppress("TooManyFunctions")
class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val networkConnection: NetworkConnection,
) {
    private val courseStructure = mutableMapOf<String, CourseStructure>()

    private val courseStatusMap = mutableMapOf<String, CourseComponentStatus>()
    private val courseDatesMap = mutableMapOf<String, CourseDatesResult>()

    suspend fun removeDownloadModel(id: String) {
        downloadDao.removeDownloadModel(id)
    }

    fun getDownloadModels() = downloadDao.getAllDataFlow().map { list ->
        list.map { it.mapToDomain() }
    }

    suspend fun getAllDownloadModels() = downloadDao.readAllData().map { it.mapToDomain() }

    suspend fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = true
    ): Flow<CourseStructure> =
        channelFlowWithAwait {
            var hasCourseStructure = false
            val cachedCourseStructure = courseStructure[courseId] ?: (
                    courseDao.getCourseStructureById(courseId)?.mapToDomain()
                    )
            if (cachedCourseStructure != null) {
                hasCourseStructure = true
                trySend(cachedCourseStructure)
            }
            val fetchRemoteCourse = !hasCourseStructure || forceRefresh
            if (networkConnection.isOnline() && fetchRemoteCourse) {
                val response = api.getCourseStructure(
                    "stale-if-error=0",
                    "v4",
                    preferencesManager.user?.username,
                    courseId
                )
                courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
                val courseDomainModel = response.mapToDomain()
                courseStructure[courseId] = courseDomainModel
                trySend(courseDomainModel)
                hasCourseStructure = true
            }
            if (!hasCourseStructure) {
                throw NoCachedDataException()
            }
        }

    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        val cachedCourseStructure = courseDao.getCourseStructureById(courseId)
        if (cachedCourseStructure != null) {
            return cachedCourseStructure.mapToDomain()
        } else {
            throw NoCachedDataException()
        }
    }

    suspend fun getCourseStructure(courseId: String, isNeedRefresh: Boolean): CourseStructure {
        if (!isNeedRefresh) courseStructure[courseId]?.let { return it }

        if (networkConnection.isOnline()) {
            val response = api.getCourseStructure(
                "stale-if-error=0",
                "v4",
                preferencesManager.user?.username,
                courseId
            )
            courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
            courseStructure[courseId] = response.mapToDomain()
        } else {
            val cachedCourseStructure = courseDao.getCourseStructureById(courseId)
            if (cachedCourseStructure != null) {
                courseStructure[courseId] = cachedCourseStructure.mapToDomain()
            } else {
                throw NoCachedDataException()
            }
        }

        return courseStructure[courseId]!!
    }

    suspend fun getEnrollmentDetailsFlow(courseId: String): Flow<CourseEnrollmentDetails> =
        channelFlowWithAwait {
            getCourseEnrollmentDetailsFromCache(courseId)?.let {
                trySend(it)
            }
            val details = getEnrollmentDetails(courseId)
            courseDao.insertCourseEnrollmentDetailsEntity(details.mapToEntity())
            trySend(details)
        }

    private suspend fun getCourseEnrollmentDetailsFromCache(courseId: String): CourseEnrollmentDetails? {
        return courseDao.getCourseEnrollmentDetailsById(id = courseId)
            ?.mapToDomain()
    }

    suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
        return api.getEnrollmentDetails(courseId = courseId).mapToDomain()
    }

    suspend fun getCourseStatusFlow(courseId: String): Flow<CourseComponentStatus> =
        channelFlowWithAwait {
            val localStatus = courseStatusMap[courseId]
            localStatus?.let { trySend(it) }

            if (networkConnection.isOnline()) {
                val username = preferencesManager.user?.username ?: ""
                val status = api.getCourseStatus(username, courseId).mapToDomain()
                courseStatusMap[courseId] = status
                trySend(status)
            } else {
                val status = localStatus ?: CourseComponentStatus("")
                trySend(status)
            }
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

    suspend fun getCourseDatesFlow(courseId: String): Flow<CourseDatesResult> =
        channelFlowWithAwait {
            val localDates = courseDatesMap[courseId]
            localDates?.let { trySend(it) }

            if (networkConnection.isOnline()) {
                val datesResult = api.getCourseDates(courseId).getCourseDatesResult()
                courseDatesMap[courseId] = datesResult
                trySend(datesResult)
            } else {
                val datesResult = localDates ?: CourseDatesResult(
                    datesSection = linkedMapOf(),
                    courseBanner = CourseDatesBannerInfo(
                        missedDeadlines = false,
                        missedGatedContent = false,
                        verifiedUpgradeLink = "",
                        contentTypeGatingEnabled = false,
                        hasEnded = false
                    )
                )
                trySend(datesResult)
            }
        }

    suspend fun getCourseDates(courseId: String) =
        api.getCourseDates(courseId).getCourseDatesResult()

    suspend fun resetCourseDates(courseId: String) =
        api.resetCourseDates(mapOf(ApiConstants.COURSE_KEY to courseId)).mapToDomain()

    suspend fun getDatesBannerInfo(courseId: String) =
        api.getDatesBannerInfo(courseId).mapToDomain()

    suspend fun getHandouts(courseId: String) = api.getHandouts(courseId).mapToDomain()

    suspend fun getAnnouncements(courseId: String) =
        api.getAnnouncements(courseId).map { it.mapToDomain() }

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

    fun getCourseProgress(courseId: String, isRefresh: Boolean): Flow<CourseProgress> =
        channelFlowWithAwait {
            if (!isRefresh) {
                val cached = courseDao.getCourseProgressById(courseId)
                if (cached != null) {
                    trySend(cached.mapToDomain())
                }
            }
            val response = api.getCourseProgress(courseId)
            courseDao.insertCourseProgressEntity(response.mapToRoomEntity(courseId))
            trySend(response.mapToDomain())
        }
}
