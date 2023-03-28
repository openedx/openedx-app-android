package com.raccoongang.newedx.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.raccoongang.core.data.model.room.CourseEntity
import com.raccoongang.core.data.model.room.CourseStructureEntity
import com.raccoongang.core.data.model.room.discovery.EnrolledCourseEntity
import com.raccoongang.core.module.db.DownloadDao
import com.raccoongang.core.module.db.DownloadModelEntity
import com.raccoongang.course.data.model.BlockDbEntity
import com.raccoongang.course.data.storage.CourseConverter
import com.raccoongang.course.data.storage.CourseDao
import com.raccoongang.dashboard.data.DashboardDao
import com.raccoongang.discovery.data.converter.DiscoveryConverter
import com.raccoongang.discovery.data.storage.DiscoveryDao

const val DATABASE_VERSION = 1
const val DATABASE_NAME = "newEdx_db"

@Database(
    entities = [
        CourseEntity::class,
        EnrolledCourseEntity::class,
        CourseStructureEntity::class,
        BlockDbEntity::class,
        DownloadModelEntity::class
    ],
    version = DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(DiscoveryConverter::class, CourseConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveryDao(): DiscoveryDao
    abstract fun courseDao(): CourseDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun downloadDao(): DownloadDao
}
