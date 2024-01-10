package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.MediaDb
import org.openedx.core.data.model.room.discovery.EnrolledCourseDataDb
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.utils.TimeUtils

data class EnrolledCourseData(
    @SerializedName("id")
    var id: String?,
    @SerializedName("name")
    var name: String?,
    @SerializedName("number")
    var number: String?,
    @SerializedName("org")
    var org: String?,
    @SerializedName("start")
    var start: String?,
    @SerializedName("start_display")
    var startDisplay: String?,
    @SerializedName("start_type")
    var startType: String?,
    @SerializedName("end")
    var end: String?,
    @SerializedName("dynamic_upgrade_deadline")
    var dynamicUpgradeDeadline: String?,
    @SerializedName("subscription_id")
    var subscriptionId: String?,
    @SerializedName("courseware_access")
    var coursewareAccess: CoursewareAccess?,
    @SerializedName("media")
    var media: Media?,
    @SerializedName("course_image")
    var courseImage: String?,
    @SerializedName("course_about")
    var courseAbout: String?,
    @SerializedName("course_sharing_utm_parameters")
    var courseSharingUtmParameters: CourseSharingUtmParameters?,
    @SerializedName("course_updates")
    var courseUpdates: String?,
    @SerializedName("course_handouts")
    var courseHandouts: String?,
    @SerializedName("discussion_url")
    var discussionUrl: String?,
    @SerializedName("video_outline")
    var videoOutline: String?,
    @SerializedName("is_self_paced")
    var isSelfPaced: Boolean?
) {

    fun mapToDomain(): EnrolledCourseData {
        return EnrolledCourseData(
            id = id ?: "",
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            start = TimeUtils.iso8601ToDate(start ?: ""),
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            end = TimeUtils.iso8601ToDate(end ?: ""),
            dynamicUpgradeDeadline = dynamicUpgradeDeadline ?: "",
            subscriptionId = subscriptionId ?: "",
            coursewareAccess = coursewareAccess?.mapToDomain(),
            media = media?.mapToDomain(),
            courseImage = courseImage ?: "",
            courseAbout = courseAbout ?: "",
            courseSharingUtmParameters = courseSharingUtmParameters?.mapToDomain()!!,
            courseUpdates = courseUpdates ?: "",
            courseHandouts = courseHandouts ?: "",
            discussionUrl = discussionUrl ?: "",
            videoOutline = videoOutline ?: "",
            isSelfPaced = isSelfPaced ?: false
        )
    }

    fun mapToRoomEntity(): EnrolledCourseDataDb {
        return EnrolledCourseDataDb(
            id = id ?: "",
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            start = start ?: "",
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            end = end ?: "",
            dynamicUpgradeDeadline = dynamicUpgradeDeadline ?: "",
            subscriptionId = subscriptionId ?: "",
            coursewareAccess = coursewareAccess?.mapToRoomEntity(),
            media = MediaDb.createFrom(media),
            courseImage = courseImage ?: "",
            courseAbout = courseAbout ?: "",
            courseSharingUtmParameters = courseSharingUtmParameters?.mapToRoomEntity()!!,
            courseUpdates = courseUpdates ?: "",
            courseHandouts = courseHandouts ?: "",
            discussionUrl = discussionUrl ?: "",
            videoOutline = videoOutline ?: "",
            isSelfPaced = isSelfPaced ?: false
        )
    }
}