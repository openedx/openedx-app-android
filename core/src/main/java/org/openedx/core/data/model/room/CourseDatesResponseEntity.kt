package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.CourseDate
import org.openedx.core.data.model.CourseDatesResponse
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseDate as DomainCourseDate
import org.openedx.core.domain.model.CourseDatesResponse as DomainCourseDatesResponse

@Entity(tableName = "course_dates_response_table")
data class CourseDatesResponseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("course_date_response_id")
    val id: Int,
    @ColumnInfo("course_date_response_count")
    val count: Int,
    @ColumnInfo("course_date_response_next")
    val next: String?,
    @ColumnInfo("course_date_response_previous")
    val previous: String?,
    @ColumnInfo("course_date_response_results")
    val results: List<CourseDateDB>
) {
    fun mapToDomain(): DomainCourseDatesResponse {
        return DomainCourseDatesResponse(
            count = count,
            next = next,
            previous = previous,
            results = results
                .mapNotNull { it.mapToDomain() }
                .sortedBy { it.dueDate }
        )
    }

    companion object {
        fun createFrom(courseDatesResponse: CourseDatesResponse): CourseDatesResponseEntity {
            with(courseDatesResponse) {
                return CourseDatesResponseEntity(
                    id = 0,
                    count = count,
                    next = next,
                    previous = previous,
                    results = results.map { CourseDateDB.createFrom(it) }
                )
            }
        }
    }
}

data class CourseDateDB(
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
        fun createFrom(courseDate: CourseDate): CourseDateDB {
            with(courseDate) {
                return CourseDateDB(
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
