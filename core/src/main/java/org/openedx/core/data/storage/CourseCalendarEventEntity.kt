package org.openedx.core.data.storage

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.room.discovery.CourseDateBlockDb
import org.openedx.core.domain.model.CourseCalendarEvent

@Entity(tableName = "course_calendar_event_table")
data class CourseCalendarEventEntity(
    @PrimaryKey
    @ColumnInfo("event_id")
    val eventId: Long,
    @ColumnInfo("course_id")
    val courseId: String,
    @Embedded
    val courseDateBlockDb: CourseDateBlockDb,
) {

    fun mapToDomain() = CourseCalendarEvent(
        courseId = courseId,
        eventId = eventId,
        courseDateBlock = courseDateBlockDb.mapToDomain()
    )
}
