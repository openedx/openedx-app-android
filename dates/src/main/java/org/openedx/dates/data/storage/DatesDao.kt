package org.openedx.dates.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.openedx.core.data.model.room.CourseDatesResponseEntity

@Dao
interface DatesDao {

    @Query("SELECT * FROM course_dates_response_table")
    suspend fun getCourseDateResponses(): List<CourseDatesResponseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseDateResponses(courseDates: CourseDatesResponseEntity)

    @Query("DELETE FROM course_dates_response_table")
    suspend fun clearCachedData()
}
