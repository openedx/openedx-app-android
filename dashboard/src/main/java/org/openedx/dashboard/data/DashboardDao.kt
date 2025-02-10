package org.openedx.dashboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.openedx.core.data.model.room.discovery.EnrolledCourseEntity

@Dao
interface DashboardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrolledCourseEntity(vararg courseEntity: EnrolledCourseEntity)

    @Query("DELETE FROM course_enrolled_table")
    suspend fun clearCachedData()

    @Query("SELECT * FROM course_enrolled_table")
    suspend fun readAllData(): List<EnrolledCourseEntity>
}
