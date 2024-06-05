package org.openedx.core.module.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.openedx.core.data.model.room.CourseCalendarEventEntity
import org.openedx.core.data.model.room.CourseCalendarStateEntity

@Dao
interface CalendarDao {

    // region CourseCalendarEventEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseCalendarEntity(vararg courseCalendarEntity: CourseCalendarEventEntity)

    @Query("DELETE FROM course_calendar_event_table WHERE course_id = :courseId")
    suspend fun deleteCourseCalendarEntitiesById(courseId: String)

    @Query("SELECT * FROM course_calendar_event_table WHERE course_id=:courseId")
    suspend fun readCourseCalendarEventsById(courseId: String): List<CourseCalendarEventEntity>

    // region CourseCalendarStateEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseCalendarStateEntity(vararg courseCalendarStateEntity: CourseCalendarStateEntity)

    @Query("SELECT * FROM course_calendar_state_table WHERE course_id=:courseId")
    suspend fun readCourseCalendarStateById(courseId: String): CourseCalendarStateEntity?

    @Query("SELECT * FROM course_calendar_state_table")
    suspend fun readAllCourseCalendarState(): List<CourseCalendarStateEntity>

    @Query(
        """
    UPDATE course_calendar_state_table
    SET
        checksum = COALESCE(:checksum, checksum),
        is_course_sync_enabled = COALESCE(:isCourseSyncEnabled, is_course_sync_enabled)
    WHERE course_id = :courseId"""
    )
    suspend fun updateCourseCalendarStateById(
        courseId: String,
        checksum: Int? = null,
        isCourseSyncEnabled: Boolean? = null
    )
}
