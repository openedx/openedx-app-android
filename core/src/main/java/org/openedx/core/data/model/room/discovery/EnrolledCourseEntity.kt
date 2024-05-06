package org.openedx.core.data.model.room.discovery

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.room.MediaDb
import org.openedx.core.domain.model.Certificate
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.utils.TimeUtils

@Entity(tableName = "course_enrolled_table")
data class EnrolledCourseEntity(
    @PrimaryKey
    @ColumnInfo("courseId")
    val courseId: String,
    @ColumnInfo("auditAccessExpires")
    val auditAccessExpires: String,
    @ColumnInfo("created")
    val created: String,
    @ColumnInfo("mode")
    val mode: String,
    @ColumnInfo("isActive")
    val isActive: Boolean,
    @Embedded
    val course: EnrolledCourseDataDb,
    @Embedded
    val certificate: CertificateDb?,
) {

    fun mapToDomain(): EnrolledCourse {
        return EnrolledCourse(
            TimeUtils.iso8601ToDate(auditAccessExpires),
            created,
            mode,
            isActive,
            course.mapToDomain(),
            certificate?.mapToDomain(),
            null
        )
    }
}

data class EnrolledCourseDataDb(
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("number")
    val number: String,
    @ColumnInfo("org")
    val org: String,
    @ColumnInfo("start")
    val start: String,
    @ColumnInfo("startDisplay")
    val startDisplay: String,
    @ColumnInfo("startType")
    val startType: String,
    @ColumnInfo("end")
    val end: String,
    @ColumnInfo("dynamicUpgradeDeadline")
    val dynamicUpgradeDeadline: String,
    @ColumnInfo("subscriptionId")
    val subscriptionId: String,
    @Embedded
    val coursewareAccess: CoursewareAccessDb?,
    @Embedded
    val media: MediaDb?,
    @ColumnInfo(name = "course_image_link")
    val courseImage: String,
    @ColumnInfo("courseAbout")
    val courseAbout: String,
    @Embedded
    val courseSharingUtmParameters: CourseSharingUtmParametersDb,
    @ColumnInfo("courseUpdates")
    val courseUpdates: String,
    @ColumnInfo("courseHandouts")
    val courseHandouts: String,
    @ColumnInfo("discussionUrl")
    val discussionUrl: String,
    @ColumnInfo("videoOutline")
    val videoOutline: String,
    @ColumnInfo("isSelfPaced")
    val isSelfPaced: Boolean
) {
    fun mapToDomain(): EnrolledCourseData {
        return EnrolledCourseData(
            id,
            name,
            number,
            org,
            TimeUtils.iso8601ToDate(start),
            startDisplay,
            startType,
            TimeUtils.iso8601ToDate(end),
            dynamicUpgradeDeadline,
            subscriptionId,
            coursewareAccess?.mapToDomain(),
            media?.mapToDomain(),
            courseImage,
            courseAbout,
            courseSharingUtmParameters.mapToDomain(),
            courseUpdates,
            courseHandouts,
            discussionUrl,
            videoOutline,
            isSelfPaced
        )
    }
}

data class CoursewareAccessDb(
    @ColumnInfo("hasAccess")
    val hasAccess: Boolean,
    @ColumnInfo("errorCode")
    val errorCode: String,
    @ColumnInfo("developerMessage")
    val developerMessage: String,
    @ColumnInfo("userMessage")
    val userMessage: String,
    @ColumnInfo("additionalContextUserMessage")
    val additionalContextUserMessage: String,
    @ColumnInfo("userFragment")
    val userFragment: String
) {

    fun mapToDomain(): CoursewareAccess {
        return CoursewareAccess(
            hasAccess,
            errorCode,
            developerMessage,
            userMessage,
            additionalContextUserMessage,
            userFragment
        )
    }

}

data class CertificateDb(
    @ColumnInfo("certificateURL")
    val certificateURL: String?
) {
    fun mapToDomain() = Certificate(certificateURL)
}

data class CourseSharingUtmParametersDb(
    @ColumnInfo("facebook")
    val facebook: String,
    @ColumnInfo("twitter")
    val twitter: String
) {
    fun mapToDomain() = CourseSharingUtmParameters(
        facebook, twitter
    )
}
