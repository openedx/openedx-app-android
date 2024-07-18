package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.domain.model.CourseCalendarState

@Entity(tableName = "course_calendar_state_table")
data class CourseCalendarStateEntity(
    @PrimaryKey
    @ColumnInfo("course_id")
    val courseId: String,
    @ColumnInfo("checksum")
    val checksum: Int = 0,
    @ColumnInfo("is_course_sync_enabled")
    val isCourseSyncEnabled: Boolean,
) {

    fun mapToDomain() = CourseCalendarState(
        checksum = checksum,
        courseId = courseId,
        isCourseSyncEnabled = isCourseSyncEnabled
    )
}
