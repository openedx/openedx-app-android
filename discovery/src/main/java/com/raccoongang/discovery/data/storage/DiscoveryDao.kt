package com.raccoongang.discovery.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raccoongang.core.data.model.room.CourseEntity

@Dao
interface DiscoveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEntity(vararg courseEntity: CourseEntity)

    @Query("DELETE FROM course_discovery_table")
    suspend fun clearCachedData()

    @Query("SELECT * FROM course_discovery_table")
    suspend fun readAllData() : List<CourseEntity>

}