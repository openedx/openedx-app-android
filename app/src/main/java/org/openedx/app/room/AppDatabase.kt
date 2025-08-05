package org.openedx.app.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity
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
import org.openedx.course.data.storage.CourseConverter
import org.openedx.dashboard.data.DashboardDao
import org.openedx.discovery.data.converter.DiscoveryConverter
import org.openedx.discovery.data.model.room.CourseEntity
import org.openedx.discovery.data.storage.DiscoveryDao

const val DATABASE_VERSION = 4
const val DATABASE_NAME = "OpenEdX_db"

@Suppress("MagicNumber")
@Database(
    entities = [
        CourseEntity::class,
        EnrolledCourseEntity::class,
        CourseStructureEntity::class,
        DownloadModelEntity::class,
        OfflineXBlockProgress::class,
        CourseCalendarEventEntity::class,
        CourseCalendarStateEntity::class,
        DownloadCoursePreview::class,
        CourseEnrollmentDetailsEntity::class,
        VideoProgressEntity::class,
        CourseProgressEntity::class,
    ],
    autoMigrations = [
        AutoMigration(1, 2),
        AutoMigration(2, 3),
        AutoMigration(3, DATABASE_VERSION),
    ],
    version = DATABASE_VERSION
)
@TypeConverters(DiscoveryConverter::class, CourseConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveryDao(): DiscoveryDao
    abstract fun courseDao(): CourseDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun downloadDao(): DownloadDao
    abstract fun calendarDao(): CalendarDao
}
