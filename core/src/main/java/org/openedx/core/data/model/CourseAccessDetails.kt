package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.CourseAccessDetailsDb
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseAccessDetails as DomainCourseAccessDetails

data class CourseAccessDetails(
    @SerializedName("audit_access_expires")
    val auditAccessExpires: String?,
) {
    fun mapToDomain(): DomainCourseAccessDetails =
        DomainCourseAccessDetails(TimeUtils.iso8601ToDate(auditAccessExpires ?: ""))

    fun mapToRoomEntity(): CourseAccessDetailsDb =
        CourseAccessDetailsDb(auditAccessExpires)
}
