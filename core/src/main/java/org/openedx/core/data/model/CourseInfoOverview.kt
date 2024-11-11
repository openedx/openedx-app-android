package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseInfoOverview as DomainCourseInfoOverview

data class CourseInfoOverview(
    @SerializedName("name")
    val name: String,
    @SerializedName("number")
    val number: String,
    @SerializedName("org")
    val org: String,
    @SerializedName("start")
    val start: String?,
    @SerializedName("start_display")
    val startDisplay: String,
    @SerializedName("start_type")
    val startType: String,
    @SerializedName("end")
    val end: String?,
    @SerializedName("is_self_paced")
    val isSelfPaced: Boolean,
    @SerializedName("media")
    var media: Media?,
    @SerializedName("course_sharing_utm_parameters")
    val courseSharingUtmParameters: CourseSharingUtmParameters,
    @SerializedName("course_about")
    val courseAbout: String,
) {
    fun mapToDomain() = DomainCourseInfoOverview(
        name = name,
        number = number,
        org = org,
        start = TimeUtils.iso8601ToDate(start ?: ""),
        startDisplay = startDisplay,
        startType = startType,
        end = TimeUtils.iso8601ToDate(end ?: ""),
        isSelfPaced = isSelfPaced,
        media = media?.mapToDomain(),
        courseSharingUtmParameters = courseSharingUtmParameters.mapToDomain(),
        courseAbout = courseAbout,
    )
}
