package org.openedx.discovery.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.room.MediaDb
import org.openedx.core.utils.TimeUtils
import org.openedx.discovery.data.model.CourseDetails
import org.openedx.discovery.domain.model.Course

@Entity(tableName = "course_discovery_table")
data class CourseEntity(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("blocksUrl")
    val blocksUrl: String,
    @ColumnInfo("courseId")
    val courseId: String,
    @ColumnInfo("effort")
    val effort: String,
    @ColumnInfo("enrollmentStart")
    val enrollmentStart: String,
    @ColumnInfo("enrollmentEnd")
    val enrollmentEnd: String,
    @ColumnInfo("hidden")
    val hidden: Boolean,
    @ColumnInfo("invitationOnly")
    val invitationOnly: Boolean,
    @Embedded
    val media: MediaDb,
    @ColumnInfo("mobileAvailable")
    val mobileAvailable: Boolean,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("number")
    val number: String,
    @ColumnInfo("org")
    val org: String,
    @ColumnInfo("pacing")
    val pacing: String,
    @ColumnInfo("shortDescription")
    val shortDescription: String,
    @ColumnInfo("start")
    val start: String,
    @ColumnInfo("end")
    val end: String,
    @ColumnInfo("startDisplay")
    val startDisplay: String,
    @ColumnInfo("startType")
    val startType: String,
    @ColumnInfo("overview")
    val overview: String,
    @ColumnInfo("isEnrolled")
    val isEnrolled: Boolean
) {

    fun mapToDomain(): Course {
        return Course(
            id = id,
            blocksUrl = blocksUrl,
            courseId = courseId,
            effort = effort,
            enrollmentStart = TimeUtils.iso8601ToDate(enrollmentStart),
            enrollmentEnd = TimeUtils.iso8601ToDate(enrollmentEnd),
            hidden = hidden,
            invitationOnly = invitationOnly,
            media = media.mapToDomain(),
            mobileAvailable = mobileAvailable,
            name = name,
            number = number,
            org = org,
            pacing = pacing,
            shortDescription = shortDescription,
            start = start,
            end = end,
            startDisplay = startDisplay,
            startType = startType,
            overview = overview,
            isEnrolled = isEnrolled
        )
    }

    companion object {
        fun createFrom(model: CourseDetails): CourseEntity {
            return CourseEntity(
                id = model.id.orEmpty(),
                blocksUrl = model.blocksUrl.orEmpty(),
                courseId = model.courseId.orEmpty(),
                effort = model.effort.orEmpty(),
                enrollmentStart = model.enrollmentStart.orEmpty(),
                enrollmentEnd = model.enrollmentEnd.orEmpty(),
                hidden = model.hidden ?: false,
                invitationOnly = model.invitationOnly ?: false,
                mobileAvailable = model.mobileAvailable ?: false,
                name = model.name.orEmpty(),
                number = model.number.orEmpty(),
                org = model.organization.orEmpty(),
                shortDescription = model.shortDescription.orEmpty(),
                start = model.start.orEmpty(),
                end = model.end.orEmpty(),
                startDisplay = model.startDisplay.orEmpty(),
                startType = model.startType.orEmpty(),
                pacing = model.pacing.orEmpty(),
                overview = model.overview.orEmpty(),
                media = MediaDb.createFrom(model.media),
                isEnrolled = model.isEnrolled ?: false
            )
        }
    }
}
