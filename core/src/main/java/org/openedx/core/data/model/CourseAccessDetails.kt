package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseAccessDetails
import org.openedx.core.utils.TimeUtils

data class CourseAccessDetails(
    @SerializedName("has_unmet_prerequisites")
    val hasUnmetPrerequisites: Boolean,
    @SerializedName("is_too_early")
    val isTooEarly: Boolean,
    @SerializedName("is_staff")
    val isStaff: Boolean,
    @SerializedName("audit_access_expires")
    val auditAccessExpires: String?,
    @SerializedName("courseware_access")
    val coursewareAccess: CoursewareAccess?,
) {
    fun mapToDomain() = CourseAccessDetails(
        hasUnmetPrerequisites = hasUnmetPrerequisites,
        isTooEarly = isTooEarly,
        isStaff = isStaff,
        auditAccessExpires = TimeUtils.iso8601ToDate(auditAccessExpires ?: ""),
        coursewareAccess = coursewareAccess?.mapToDomain(),
    )
}
