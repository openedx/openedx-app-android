package org.openedx.app.room

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.data.model.room.CourseDateEntity
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.data.model.room.CourseProgressEntity
import org.openedx.core.data.model.room.CourseStructureEntity
import org.openedx.core.data.model.room.DownloadCoursePreview
import org.openedx.core.data.model.room.OfflineXBlockProgress
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.data.model.room.discovery.EnrolledCourseEntity
import org.openedx.core.data.storage.CourseDao
import org.openedx.core.module.db.CalendarDao
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.dashboard.data.DashboardDao
import org.openedx.dates.data.storage.DatesDao
import org.openedx.discovery.data.model.room.CourseEntity
import org.openedx.discovery.data.storage.DiscoveryDao

/*
 * The DAOs the rest of the app injects. Each call goes to the selected instance's database,
 * resolved once per call so one call never spans two databases. Default @Transaction methods
 * are forwarded explicitly to the generated DAO, which is what runs them in a transaction.
 * Flows follow the selection: they switch to the new database after every site switch.
 */

class InstanceScopedCourseDao(private val databases: InstanceDatabaseProvider) : CourseDao {
    private val dao get() = databases.current.courseDao()

    override suspend fun getCourseStructureById(id: String) = dao.getCourseStructureById(id)

    override suspend fun insertCourseStructureEntity(vararg courseStructureEntity: CourseStructureEntity) =
        dao.insertCourseStructureEntity(*courseStructureEntity)

    override suspend fun clearCachedData() = dao.clearCachedData()

    override suspend fun clearCourseStructure() = dao.clearCourseStructure()

    override suspend fun clearVideoProgress() = dao.clearVideoProgress()

    override suspend fun clearEnrollmentCachedData() = dao.clearEnrollmentCachedData()

    override suspend fun clearCourseProgressData() = dao.clearCourseProgressData()

    override suspend fun insertCourseEnrollmentDetailsEntity(
        vararg courseEnrollmentDetailsEntity: CourseEnrollmentDetailsEntity
    ) = dao.insertCourseEnrollmentDetailsEntity(*courseEnrollmentDetailsEntity)

    override suspend fun getCourseEnrollmentDetailsById(id: String) = dao.getCourseEnrollmentDetailsById(id)

    override suspend fun insertVideoProgressEntity(vararg videoProgressEntity: VideoProgressEntity) =
        dao.insertVideoProgressEntity(*videoProgressEntity)

    override suspend fun getVideoProgressByBlockId(blockId: String) = dao.getVideoProgressByBlockId(blockId)

    override suspend fun insertCourseProgressEntity(vararg courseProgressEntity: CourseProgressEntity) =
        dao.insertCourseProgressEntity(*courseProgressEntity)

    override suspend fun getCourseProgressById(id: String) = dao.getCourseProgressById(id)
}

@OptIn(ExperimentalCoroutinesApi::class)
class InstanceScopedDownloadDao(private val databases: InstanceDatabaseProvider) : DownloadDao {
    private val dao get() = databases.current.downloadDao()

    override suspend fun removeDownloadModel(id: String) = dao.removeDownloadModel(id)

    override suspend fun insertDownloadModel(downloadModelEntities: List<DownloadModelEntity>) =
        dao.insertDownloadModel(downloadModelEntities)

    override suspend fun updateDownloadModel(downloadModelEntity: DownloadModelEntity) =
        dao.updateDownloadModel(downloadModelEntity)

    override fun getAllDataFlow(): Flow<List<DownloadModelEntity>> =
        databases.currentFlow.flatMapLatest { it.downloadDao().getAllDataFlow() }

    override suspend fun readAllData() = dao.readAllData()

    override fun readAllDataByIds(ids: List<String>): Flow<List<DownloadModelEntity>> =
        databases.currentFlow.flatMapLatest { it.downloadDao().readAllDataByIds(ids) }

    override suspend fun removeAllDownloadModels(ids: List<String>) = dao.removeAllDownloadModels(ids)

    override suspend fun getDownloadModelsByCourseIds(courseId: String) = dao.getDownloadModelsByCourseIds(courseId)

    override suspend fun insertOfflineXBlockProgress(offlineXBlockProgress: OfflineXBlockProgress) =
        dao.insertOfflineXBlockProgress(offlineXBlockProgress)

    override suspend fun getOfflineXBlockProgress(id: String) = dao.getOfflineXBlockProgress(id)

    override suspend fun getAllOfflineXBlockProgress() = dao.getAllOfflineXBlockProgress()

    override suspend fun removeOfflineXBlockProgress(ids: List<String>) = dao.removeOfflineXBlockProgress(ids)

    override suspend fun clearOfflineProgress() = dao.clearOfflineProgress()

    override suspend fun insertDownloadCoursePreview(downloadCoursePreview: List<DownloadCoursePreview>) =
        dao.insertDownloadCoursePreview(downloadCoursePreview)

    override fun getDownloadCoursesPreview() = dao.getDownloadCoursesPreview()
}

class InstanceScopedCalendarDao(private val databases: InstanceDatabaseProvider) : CalendarDao {
    private val dao get() = databases.current.calendarDao()

    override suspend fun insertCourseCalendarEntity(vararg courseCalendarEntity: CourseCalendarEventEntity) =
        dao.insertCourseCalendarEntity(*courseCalendarEntity)

    override suspend fun deleteCourseCalendarEntitiesById(courseId: String) =
        dao.deleteCourseCalendarEntitiesById(courseId)

    override suspend fun readCourseCalendarEventsById(courseId: String) = dao.readCourseCalendarEventsById(courseId)

    override suspend fun readAllCourseCalendarEvents() = dao.readAllCourseCalendarEvents()

    override suspend fun clearCourseCalendarEventsCachedData() = dao.clearCourseCalendarEventsCachedData()

    override suspend fun insertCourseCalendarStateEntity(vararg courseCalendarStateEntity: CourseCalendarStateEntity) =
        dao.insertCourseCalendarStateEntity(*courseCalendarStateEntity)

    override suspend fun readCourseCalendarStateById(courseId: String) = dao.readCourseCalendarStateById(courseId)

    override suspend fun readAllCourseCalendarState() = dao.readAllCourseCalendarState()

    override suspend fun clearCourseCalendarStateCachedData() = dao.clearCourseCalendarStateCachedData()

    override suspend fun deleteCourseCalendarStateById(courseId: String) = dao.deleteCourseCalendarStateById(courseId)

    override suspend fun resetChecksums() = dao.resetChecksums()

    override suspend fun updateCourseCalendarStateById(
        courseId: String,
        checksum: Int?,
        isCourseSyncEnabled: Boolean?,
    ) = dao.updateCourseCalendarStateById(courseId, checksum, isCourseSyncEnabled)

    override suspend fun clearCachedData() = dao.clearCachedData()
}

class InstanceScopedDiscoveryDao(private val databases: InstanceDatabaseProvider) : DiscoveryDao {
    private val dao get() = databases.current.discoveryDao()

    override suspend fun getCourseById(id: String) = dao.getCourseById(id)

    override suspend fun insertCourseEntity(vararg courseEntity: CourseEntity) = dao.insertCourseEntity(*courseEntity)

    override suspend fun updateCourseEntity(courseEntity: CourseEntity) = dao.updateCourseEntity(courseEntity)

    override suspend fun clearCachedData() = dao.clearCachedData()

    override suspend fun readAllData() = dao.readAllData()
}

class InstanceScopedDashboardDao(private val databases: InstanceDatabaseProvider) : DashboardDao {
    private val dao get() = databases.current.dashboardDao()

    override suspend fun insertEnrolledCourseEntity(vararg courseEntity: EnrolledCourseEntity) =
        dao.insertEnrolledCourseEntity(*courseEntity)

    override suspend fun clearCachedData() = dao.clearCachedData()

    override suspend fun readAllData() = dao.readAllData()
}

class InstanceScopedDatesDao(private val databases: InstanceDatabaseProvider) : DatesDao {
    private val dao get() = databases.current.datesDao()

    override suspend fun getCourseDates() = dao.getCourseDates()

    override suspend fun getCourseDates(limit: Int) = dao.getCourseDates(limit)

    override suspend fun insertCourseDates(courseDates: List<CourseDateEntity>) = dao.insertCourseDates(courseDates)

    override suspend fun clearCachedData() = dao.clearCachedData()
}
