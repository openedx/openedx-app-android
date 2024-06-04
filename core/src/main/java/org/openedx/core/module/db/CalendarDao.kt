package org.openedx.core.module.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.openedx.core.data.storage.CourseCalendarEventEntity

@Dao
interface CalendarDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseCalendarEntity(vararg courseCalendarEntity: CourseCalendarEventEntity)

    @Query("DELETE FROM course_calendar_event_table")
    suspend fun clearCachedData()

    @Query("SELECT * FROM course_calendar_event_table")
    suspend fun readAllData(): List<CourseCalendarEventEntity>
}
