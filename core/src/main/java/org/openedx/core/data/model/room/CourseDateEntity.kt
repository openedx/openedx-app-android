package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.CourseDate
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseDate as DomainCourseDate

@Entity(tableName = "course_dates_table")
data class CourseDateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Int,
    @ColumnInfo("first_component_block_id")
    val firstComponentBlockId: String?,
    @ColumnInfo("course_id")
    val courseId: String,
    @ColumnInfo("due_date")
    val dueDate: String?,
    @ColumnInfo("assignment_title")
    val assignmentTitle: String?,
    @ColumnInfo("learner_has_access")
    val learnerHasAccess: Boolean?,
    @ColumnInfo("relative")
    val relative: Boolean?,
    @ColumnInfo("course_name")
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
