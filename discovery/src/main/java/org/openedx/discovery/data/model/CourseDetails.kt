package org.openedx.discovery.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.Media
import org.openedx.core.utils.TimeUtils
import org.openedx.discovery.domain.model.Course

data class CourseDetails(
    @SerializedName("blocks_url")
    val blocksUrl: String?,
    @SerializedName("course_id")
    val courseId: String?,
    @SerializedName("effort")
    val effort: String?,
    @SerializedName("end")
    val end: String?,
    @SerializedName("enrollment_end")
    val enrollmentEnd: String?,
    @SerializedName("enrollment_start")
    val enrollmentStart: String?,
    @SerializedName("hidden")
    val hidden: Boolean?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("invitation_only")
    val invitationOnly: Boolean?,
    @SerializedName("media")
    val media: Media?,
    @SerializedName("mobile_available")
    val mobileAvailable: Boolean?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("number")
    val number: String?,
    @SerializedName("org")
    val organization: String?,
    @SerializedName("pacing")
    val pacing: String?,
    @SerializedName("short_description")
    val shortDescription: String?,
    @SerializedName("start")
    val start: String?,
    @SerializedName("start_display")
    val startDisplay: String?,
    @SerializedName("start_type")
    val startType: String?,
    @SerializedName("overview")
    val overview: String?,
    @SerializedName("is_enrolled")
    val isEnrolled: Boolean?,
) {

    fun mapToDomain(): Course {
        return Course(
            id = id.orEmpty(),
            blocksUrl = blocksUrl.orEmpty(),
            courseId = courseId.orEmpty(),
            effort = effort.orEmpty(),
            enrollmentStart = parseEnrollmentStartDate(),
            enrollmentEnd = parseEnrollmentEndDate(),
            hidden = hidden ?: false,
            invitationOnly = invitationOnly ?: false,
            mobileAvailable = mobileAvailable ?: false,
            name = name.orEmpty(),
            number = number.orEmpty(),
            org = organization.orEmpty(),
            shortDescription = shortDescription.orEmpty(),
            start = start.orEmpty(),
            end = end.orEmpty(),
            startDisplay = startDisplay.orEmpty(),
            startType = startType.orEmpty(),
            pacing = pacing.orEmpty(),
            overview = overview.orEmpty(),
            isEnrolled = isEnrolled ?: false,
            media = mapMediaToDomain()
        )
    }

    private fun parseEnrollmentStartDate() = TimeUtils.iso8601ToDate(enrollmentStart.orEmpty())

    private fun parseEnrollmentEndDate() = TimeUtils.iso8601ToDate(enrollmentEnd.orEmpty())

    private fun mapMediaToDomain() = media?.mapToDomain() ?: org.openedx.core.domain.model.Media()
}
