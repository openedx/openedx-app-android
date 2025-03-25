package org.openedx.dates.data.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.CourseDate
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseDate as DomainCourseDate

@Entity(tableName = "course_date_table")
data class CourseDateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("course_date_id")
    val id: Int,
    @ColumnInfo("course_date_first_component_block_id")
    val firstComponentBlockId: String?,
    @ColumnInfo("course_date_courseId")
    val courseId: String,
    @ColumnInfo("course_date_dueDate")
    val dueDate: String?,
    @ColumnInfo("course_date_assignmentTitle")
    val assignmentTitle: String?,
    @ColumnInfo("course_date_learnerHasAccess")
    val learnerHasAccess: Boolean?,
    @ColumnInfo("course_date_relative")
    val relative: Boolean?,
    @ColumnInfo("course_date_courseName")
    val courseName: String?,
) {

    fun mapToDomain(): DomainCourseDate? {
        val dueDate = TimeUtils.iso8601ToDate(dueDate ?: "")
        return DomainCourseDate(
            courseId = courseId,
            firstComponentBlockId = firstComponentBlockId ?: "",
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
                    id = 0,
                    courseId = courseId,
                    firstComponentBlockId = firstComponentBlockId,
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
