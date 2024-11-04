package org.openedx.discovery.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.openedx.discovery.data.model.room.CourseEntity

@Dao
interface DiscoveryDao {

    @Query("SELECT * FROM course_discovery_table WHERE id=:id")
    suspend fun getCourseById(id: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEntity(vararg courseEntity: CourseEntity)

    @Update
    suspend fun updateCourseEntity(courseEntity: CourseEntity)

    @Query("DELETE FROM course_discovery_table")
    suspend fun clearCachedData()

    @Query("SELECT * FROM course_discovery_table")
    suspend fun readAllData(): List<CourseEntity>
}
