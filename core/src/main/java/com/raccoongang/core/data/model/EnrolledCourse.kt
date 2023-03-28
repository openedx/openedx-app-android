package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.room.discovery.EnrolledCourseEntity
import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.core.utils.TimeUtils

data class EnrolledCourse(
    @SerializedName("audit_access_expires")
    val auditAccessExpires: String?,
    @SerializedName("created")
    val created: String?,
    @SerializedName("mode")
    val mode: String?,
    @SerializedName("is_active")
    val isActive: Boolean?,
    @SerializedName("course")
    val course: EnrolledCourseData?,
    @SerializedName("certificate")
    val certificate: Certificate?
) {
    fun mapToDomain(): EnrolledCourse {
        return EnrolledCourse(
            auditAccessExpires = TimeUtils.iso8601ToDate(auditAccessExpires ?: ""),
            created = created ?: "",
            mode = mode ?: "",
            isActive = isActive ?: false,
            course = course?.mapToDomain()!!,
            certificate = certificate?.mapToDomain()
        )
    }

    fun mapToRoomEntity(): EnrolledCourseEntity {
        return EnrolledCourseEntity(
            courseId = course?.id ?: "",
            auditAccessExpires = auditAccessExpires ?: "",
            created = created ?: "",
            mode = mode ?: "",
            isActive = isActive ?: false,
            course = course?.mapToRoomEntity()!!,
            certificate = certificate?.mapToRoomEntity()
        )
    }
}
