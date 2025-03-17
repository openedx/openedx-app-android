package org.openedx.dates.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DatesDao {

    @Query("SELECT * FROM course_date_table")
    suspend fun getCourseDateEntities(): List<CourseDateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseDateEntities(courseDate: List<CourseDateEntity>)

    @Query("DELETE FROM course_date_table")
    suspend fun clearCachedData()
}
