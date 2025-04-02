package org.openedx.dates.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.openedx.core.data.model.room.CourseDateEntity

@Dao
interface DatesDao {

    @Query("SELECT * FROM course_dates_table")
    suspend fun getCourseDates(): List<CourseDateEntity>

    @Query("SELECT * FROM course_dates_table LIMIT :limit")
    suspend fun getCourseDates(limit: Int): List<CourseDateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseDates(courseDates: List<CourseDateEntity>)

    @Query("DELETE FROM course_dates_table")
    suspend fun clearCachedData()
}
