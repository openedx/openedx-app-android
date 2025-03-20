package org.openedx.dates.data.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.CourseDate
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseDate as DomainCourseDate

@Entity(tableName = "course_date_table")
data class CourseDateEntity(
    @PrimaryKey
    @ColumnInfo("assignmentBlockId")
    val assignmentBlockId: String,
    @ColumnInfo("courseId")
    val courseId: String,
    @ColumnInfo("dueDate")
    val dueDate: String?,
    @ColumnInfo("assignmentTitle")
    val assignmentTitle: String?,
    @ColumnInfo("learnerHasAccess")
    val learnerHasAccess: Boolean?,
    @ColumnInfo("relative")
    val relative: Boolean?,
    @ColumnInfo("courseName")
    val courseName: String?,
) {

    fun mapToDomain(): DomainCourseDate? {
        val dueDate = TimeUtils.iso8601ToDate(dueDate ?: "")
        return DomainCourseDate(
            courseId = courseId,
            assignmentBlockId = assignmentBlockId,
            dueDate = dueDate ?: return null,
            assignmentTitle = assignmentTitle ?: "",
            learnerHasAccess = learnerHasAccess ?: false,
            relative = relative ?: false,
            courseName = courseName ?: ""
        )
    }

    companion object {
        fun createFrom(courseDate: CourseDate): CourseDateEntity {
            with(courseDate) {
                return CourseDateEntity(
                    courseId = courseId,
                    assignmentBlockId = assignmentBlockId,
                    dueDate = dueDate,
                    assignmentTitle = assignmentTitle,
                    learnerHasAccess = learnerHasAccess,
                    relative = relative,
                    courseName = courseName
                )
            }
        }
    }
}
