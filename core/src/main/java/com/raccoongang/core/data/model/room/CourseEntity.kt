package com.raccoongang.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raccoongang.core.data.model.CourseDetails
import com.raccoongang.core.domain.model.Course
import com.raccoongang.core.utils.TimeUtils

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
            with(model) {
                return CourseEntity(
                    id = id ?: "",
                    blocksUrl = blocksUrl ?: "",
                    courseId = courseId ?: "",
                    effort = effort ?: "",
                    enrollmentStart = enrollmentStart ?: "",
                    enrollmentEnd = enrollmentEnd ?: "",
                    hidden = hidden ?: false,
                    invitationOnly = invitationOnly ?: false,
                    mobileAvailable = mobileAvailable ?: false,
                    name = name ?: "",
                    number = number ?: "",
                    org = org ?: "",
                    shortDescription = shortDescription ?: "",
                    start = start ?: "",
                    end = end ?: "",
                    startDisplay = startDisplay ?: "",
                    startType = startType ?: "",
                    pacing = pacing ?: "",
                    overview = overview ?: "",
                    media = MediaDb.createFrom(media),
                    isEnrolled = isEnrolled ?: false
                )
            }
        }
    }

}